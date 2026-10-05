package com.antioxidant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MappingGenerator {
    public Result generate(List<CustomItem> input) {
        return generate(input, List.of());
    }

    public Result generate(List<CustomItem> input, List<BedrockCustomBlock> blockInput) {
        List<CustomItem> sorted = input.stream()
                .sorted(Comparator.comparing((CustomItem item) -> item.texture() == null || item.texture().isBlank())
                        .thenComparingInt(item -> "resource-pack".equals(item.source()) ? 0 : 1)
                        .thenComparing(CustomItem::stableKey))
                .toList();
        List<BedrockCustomBlock> sortedBlocks = blockInput.stream().sorted(Comparator.comparing(BedrockCustomBlock::stableKey)).toList();
        List<CustomItem> items = new ArrayList<>();
        List<BedrockCustomBlock> blocks = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> identifiers = new HashSet<>();
        Set<String> javaSelectors = new HashSet<>();
        Map<String, Set<Integer>> reservedCustomModelData = new LinkedHashMap<>();
        Map<String, Set<Integer>> assignedCustomModelData = new LinkedHashMap<>();
        Map<String, List<Object>> mappings = new LinkedHashMap<>();

        for (CustomItem item : sorted) {
            String base = normalizeBase(item.baseItem());
            if (base != null && item.customModelData() != null
                    && (item.itemModel() == null || item.itemModel().isBlank())) {
                reservedCustomModelData.computeIfAbsent(base, ignored -> new HashSet<>()).add(item.customModelData());
            }
        }

        for (CustomItem original : sorted) {
            String base = normalizeBase(original.baseItem());
            if (base == null) {
                warnings.add("Skipped " + original.javaIdentifier() + ": no usable Java base item was detected");
                continue;
            }
            if (original.customModelData() == null && (original.itemModel() == null || original.itemModel().isBlank())) {
                warnings.add("Skipped " + original.javaIdentifier() + ": neither item_model nor custom_model_data is known");
                continue;
            }
            boolean hasItemModel = original.itemModel() != null && !original.itemModel().isBlank();
            CustomItem mapped = original;
            if (!hasItemModel) {
                Set<Integer> assigned = assignedCustomModelData.computeIfAbsent(base, ignored -> new HashSet<>());
                int customModelData = original.customModelData();
                if (!assigned.add(customModelData)) {
                    Set<Integer> reserved = reservedCustomModelData.computeIfAbsent(base, ignored -> new HashSet<>());
                    int replacement = nextCustomModelData(reserved);
                    reserved.add(replacement);
                    assigned.add(replacement);
                    mapped = original.withCustomModelData(replacement);
                    warnings.add("Reassigned duplicate custom_model_data for " + original.javaIdentifier() + ": "
                            + customModelData + " -> " + replacement + " (update the Java item to use the new value)");
                }
            }
            String selector = base + "|" + (hasItemModel ? "item_model:" + original.itemModel()
                    : "custom_model_data:" + mapped.customModelData());
            if (!javaSelectors.add(selector)) {
                warnings.add("Skipped duplicate Java selector for " + original.javaIdentifier() + ": " + selector);
                continue;
            }
            String rawId = "smc:" + slug(original.source()) + "_" + slug(original.javaIdentifier());
            String bedrockId = rawId;
            int collision = 2;
            while (!identifiers.add(bedrockId)) bedrockId = rawId + "_" + collision++;
            CustomItem item = mapped.withBedrockIdentifier(bedrockId);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", hasItemModel ? "definition" : "legacy");
            if (hasItemModel) entry.put("model", original.itemModel());
            else entry.put("custom_model_data", mapped.customModelData());
            entry.put("bedrock_identifier", bedrockId);
            entry.put("bedrock_options", Map.of("icon", geyserIconKey(bedrockId)));
            mappings.computeIfAbsent(base, ignored -> new ArrayList<>()).add(entry);
            items.add(item);
        }

        Map<String, Map<String, Object>> blockMappings = new LinkedHashMap<>();
        for (BedrockCustomBlock original : sortedBlocks) {
            if (original.javaBaseBlock() == null || original.javaStates().isEmpty()) {
                warnings.add("Skipped block " + original.javaIdentifier() + ": Java block identifier or state is missing");
                continue;
            }
            if (original.texture() == null || original.texture().isBlank()) {
                warnings.add("Skipped block " + original.javaIdentifier() + ": no block texture was resolved");
                continue;
            }
            CustomItem matchingItem = items.stream()
                    .filter(item -> item.source().equals(original.source()) && item.javaIdentifier().equals(original.javaIdentifier()))
                    .findFirst().orElse(null);
            String bedrockId = matchingItem == null ? "smc:" + slug(original.source()) + "_" + slug(original.javaIdentifier()) : matchingItem.bedrockIdentifier();
            if (matchingItem == null) {
                String rawId = bedrockId;
                int collision = 2;
                while (!identifiers.add(bedrockId)) bedrockId = rawId + "_" + collision++;
            }
            BedrockCustomBlock block = original.withBedrockIdentifier(bedrockId);
            Map<String, Object> stateOverride = new LinkedHashMap<>();
            stateOverride.put("name", bedrockId);
            stateOverride.put("display_name", displayName(original.javaIdentifier()));
            stateOverride.put("unit_cube", true);
            stateOverride.put("material_instances", Map.of("*", Map.of(
                    "texture", geyserIconKey(bedrockId), "render_method", "opaque",
                    "face_dimming", true, "ambient_occlusion", true)));
            Map<String, Object> baseMapping = blockMappings.computeIfAbsent(original.javaBaseBlock(), ignored -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("name", "smc_custom_block");
                value.put("only_override_states", true);
                value.put("state_overrides", new LinkedHashMap<String, Object>());
                return value;
            });
            @SuppressWarnings("unchecked")
            Map<String, Object> stateOverrides = (Map<String, Object>) baseMapping.get("state_overrides");
            String stateKey = original.javaStates().entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(java.util.stream.Collectors.joining(","));
            stateOverrides.put(stateKey, stateOverride);
            blocks.add(block);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("format_version", 2);
        root.put("items", mappings);
        Map<String, Object> blockRoot = new LinkedHashMap<>();
        blockRoot.put("format_version", 1);
        if (!blockMappings.isEmpty()) blockRoot.put("blocks", blockMappings);
        return new Result(root, blockRoot, List.copyOf(items), List.copyOf(blocks), List.copyOf(warnings));
    }

    private int nextCustomModelData(Set<Integer> used) {
        long candidate = used.stream().mapToLong(Integer::longValue).max().orElse(0L) + 1L;
        if (candidate > Integer.MAX_VALUE) {
            candidate = 0;
            while (candidate <= Integer.MAX_VALUE && used.contains((int) candidate)) candidate++;
        }
        if (candidate > Integer.MAX_VALUE) throw new IllegalStateException("No unused custom_model_data value is available");
        return (int) candidate;
    }

    private String displayName(String identifier) {
        if (identifier == null) return "Custom Block";
        String value = identifier.substring(identifier.lastIndexOf(':') + 1).replace('_', ' ');
        return value.isBlank() ? "Custom Block" : value;
    }

    public static String slug(String value) {
        String slug = value == null ? "item" : value.toLowerCase(java.util.Locale.ROOT)
                .replace(':', '_').replaceAll("[^a-z0-9_.-]+", "_")
                .replaceAll("_+", "_").replaceAll("^[_.-]+|[_.-]+$", "");
        return slug.isBlank() ? "item" : slug;
    }

    public static String geyserIconKey(String identifier) {
        return identifier.replace(':', '.').replace('/', '_');
    }

    private String normalizeBase(String base) {
        if (base == null || base.isBlank()) return null;
        String normalized = base.contains(":") ? base.toLowerCase(java.util.Locale.ROOT) : "minecraft:" + base.toLowerCase(java.util.Locale.ROOT);
        return normalized.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") ? normalized : null;
    }

    public record Result(Map<String, Object> json, Map<String, Object> blockJson, List<CustomItem> items, List<BedrockCustomBlock> blocks,
                         List<String> warnings) {}
}