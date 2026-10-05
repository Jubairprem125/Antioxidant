package com.antioxidant;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ConversionService {
    private final JavaPlugin plugin;
    private final CompatibilityManager compatibility;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Antioxidant-Worker");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile ConversionResult latest;

    public ConversionService(JavaPlugin plugin, CompatibilityManager compatibility) {
        this.plugin = plugin;
        this.compatibility = compatibility;
    }

    public boolean start(boolean includeItemsAdder, boolean includeNexo, Runnable completion) {
        if (!running.compareAndSet(false, true)) return false;
        compatibility.detect();
        List<CustomItem> integrationItems = compatibility.snapshotItems(includeItemsAdder, includeNexo);
        List<BedrockCustomBlock> integrationBlocks = compatibility.snapshotBlocks(includeItemsAdder);
        List<Path> roots = sourceRoots(plugin.getDataFolder().toPath());
        boolean generatePack = plugin.getConfig().getBoolean("enable-auto-pack", true);
        worker.submit(() -> {
            try {
                latest = convert(integrationItems, integrationBlocks, roots, generatePack);
                plugin.getLogger().info("Scan complete: " + latest.customItems() + " custom items, " + latest.mappings() + " mappings, " + latest.textures() + " textures; pack " + latest.packStatus() + ".");
            } catch (Exception exception) {
                plugin.getLogger().severe("Conversion failed: " + exception.getMessage());
                latest = ConversionResult.failed(exception.getMessage());
            } finally {
                running.set(false);
                if (completion != null) Bukkit.getScheduler().runTask(plugin, completion);
            }
        });
        return true;
    }

    public ConversionResult latest() { return latest; }
    public boolean running() { return running.get(); }

    public void shutdown() {
        worker.shutdownNow();
    }

    private ConversionResult convert(List<CustomItem> integrationItems, List<BedrockCustomBlock> integrationBlocks,
                                     List<Path> roots, boolean generatePack) throws IOException {
        Path pluginFolder = plugin.getDataFolder().toPath();
        Path generated = pluginFolder.resolve("generated");
        Path logFolder = pluginFolder.resolve("logs");
        Path cacheFolder = pluginFolder.resolve("cache");
        Files.createDirectories(generated);
        Files.createDirectories(logFolder);

        List<String> warnings = new ArrayList<>();
        ResourcePackScanner scanner = new ResourcePackScanner();
        ResourcePackScanner.PackIndex index = scanner.scan(roots, warnings::add);
        List<CustomItem> allItems = new ArrayList<>(index.discoveredItems());
        allItems.addAll(integrationItems);
        Map<String, CustomItem> unique = new LinkedHashMap<>();
        allItems.stream().sorted(Comparator.comparing(CustomItem::stableKey)).forEach(item -> unique.putIfAbsent(item.stableKey(), index.resolve(item)));
        List<BedrockCustomBlock> blocks = integrationBlocks.stream().map(index::resolve).toList();

        MappingGenerator.Result mapping = new MappingGenerator().generate(List.copyOf(unique.values()), blocks);
        warnings.addAll(mapping.warnings());
        Path mappingsFile = generated.resolve("mappings/antioxidant-mappings.json");
        Path blockMappingsFile = generated.resolve("mappings/antioxidant-block-mappings.json");
        Files.createDirectories(mappingsFile.getParent());
        Files.writeString(mappingsFile, Json.stringify(mapping.json()), StandardCharsets.UTF_8);
        if (!mapping.blocks().isEmpty()) {
            Files.writeString(blockMappingsFile, Json.stringify(mapping.blockJson()), StandardCharsets.UTF_8);
        }

        HashCache cache = new HashCache(cacheFolder.resolve("sha256.properties"));
        BedrockPackGenerator.PackResult pack = new BedrockPackGenerator().generate(pluginFolder, generated,
                mapping.items(), mapping.blocks(), index, cache, generatePack);
        warnings.addAll(pack.warnings());

        String packStatus = "generated";
        Path geyserConfigDirectory = compatibility.geyserConfigDirectory();
        if (geyserConfigDirectory != null) {
            try {
                Path customMappingsDirectory = geyserConfigDirectory.resolve("custom_mappings");
                Files.createDirectories(customMappingsDirectory);
                Files.copy(mappingsFile, customMappingsDirectory.resolve("antioxidant-mappings.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                if (!mapping.blocks().isEmpty()) {
                    Files.copy(blockMappingsFile, customMappingsDirectory.resolve("antioxidant-block-mappings.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                packStatus = "mappings staged for Geyser; reload or restart Geyser to load them";
            } catch (IOException exception) {
                warnings.add("Could not stage custom mappings in Geyser's custom_mappings directory: " + exception.getMessage());
            }
        }
        if (generatePack) {
            Path geyserDirectory = compatibility.geyserPackDirectory();
            if (geyserDirectory != null && Files.isRegularFile(pack.resourcePackArchive())) {
                try {
                    Files.createDirectories(geyserDirectory);
                    Files.copy(pack.resourcePackArchive(), geyserDirectory.resolve("antioxidant-resource-pack.mcpack"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    packStatus = packStatus.equals("generated") ? "resource pack staged for Geyser" : packStatus + "; resource pack staged for Geyser";
                } catch (IOException exception) {
                    warnings.add("Could not stage the pack in Geyser's pack directory: " + exception.getMessage());
                }
            }
        } else {
            packStatus = "pack generation disabled";
        }

        ConversionResult result = new ConversionResult(true, unique.size(), mapping.items().size(), mapping.json().get("items") instanceof Map<?, ?> map ? map.size() : 0,
                pack.textures(), pack.models(), warnings, packStatus, mappingsFile.toString(), pack.archive().toString(), pack.packUuid());
        writeReport(logFolder.resolve("conversion-report.json"), result, mapping.items());
        return result;
    }

    private List<Path> sourceRoots(Path pluginFolder) {
        Path plugins = pluginFolder.getParent();
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        roots.add(plugin.getServer().getWorldContainer().toPath().resolve("resourcepacks"));
        roots.add(pluginFolder.resolve("source-packs"));
        roots.add(plugins.resolve("ItemsAdder"));
        roots.add(plugins.resolve("Nexo"));
        roots.add(plugins.resolve("Oraxen"));
        roots.add(plugins.resolve("ItemsAdder/output/generated.zip"));
        return roots.stream().map(path -> path.toAbsolutePath().normalize()).toList();
    }

    private void writeReport(Path path, ConversionResult result, List<CustomItem> items) throws IOException {
        List<Object> itemReports = new ArrayList<>();
        for (CustomItem item : items) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("detected_item", item.javaIdentifier());
            entry.put("source", item.source());
            entry.put("java_identifier", item.baseItem());
            entry.put("bedrock_identifier", item.bedrockIdentifier());
            entry.put("mapping_type", item.itemModel() == null ? "legacy" : "definition");
            entry.put("item_model", item.itemModel());
            entry.put("model", item.model());
            entry.put("texture", item.texture());
            entry.put("conversion_result", item.texture() == null ? "mapping-only; texture not resolved" : "texture copied when readable");
            entry.put("warnings", item.warnings());
            itemReports.add(entry);
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("summary", Map.of("custom_items", result.customItems(), "mappings", result.mappings(), "textures_converted", result.textures(), "models_converted", result.models(), "pack_status", result.packStatus()));
        report.put("items", itemReports);
        report.put("warnings", result.warnings());
        Files.writeString(path, Json.stringify(report), StandardCharsets.UTF_8);
    }

    public record ConversionResult(boolean successful, int customItems, int mappings, int baseItems,
                                   int textures, int models, List<String> warnings, String packStatus,
                                   String mappingsPath, String packPath, String packUuid) {
        static ConversionResult failed(String message) {
            return new ConversionResult(false, 0, 0, 0, 0, 0, List.of(message), "failed", "", "", "");
        }
    }
}