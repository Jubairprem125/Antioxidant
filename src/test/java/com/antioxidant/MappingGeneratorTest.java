package com.antioxidant;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MappingGeneratorTest {
    public static void run() {
        generatesLegacyAndDefinitionMappingsWithUniqueIdentifiers();
        remapsDuplicateCustomModelDataValues();
        generatesNamedCustomBlockStateOverrides();
        slugSanitizesIdentifiers();
    }

    private static void generatesLegacyAndDefinitionMappingsWithUniqueIdentifiers() {
        CustomItem legacy = new CustomItem("ItemsAdder", "items:ruby", "minecraft:stick", 17, null,
                "items:item/ruby", "items/textures/ruby.png", null, List.of());
        CustomItem modern = new CustomItem("Nexo", "items:ruby", "minecraft:stick", null, "custom:ruby",
                "custom:item/ruby", "custom:textures/ruby.png", null, List.of());
        MappingGenerator.Result result = new MappingGenerator().generate(List.of(legacy, modern));
        JsonTest.check(result.items().size() == 2, "both mapping types were generated");
        JsonTest.check(!Objects.equals(result.items().get(0).bedrockIdentifier(), result.items().get(1).bedrockIdentifier()), "Bedrock identifiers are unique");
        JsonTest.check(Json.array(Json.object(Json.object(result.json()).get("items")).get("minecraft:stick")).size() == 2, "same-base mappings are retained");
    }

    private static void slugSanitizesIdentifiers() {
        JsonTest.check("my_items_ruby_sword".equals(MappingGenerator.slug("My Items:ruby sword")), "identifiers are sanitized");
    }

    private static void remapsDuplicateCustomModelDataValues() {
        CustomItem apiItem = new CustomItem("itemsadder", "minecraft:stick#17", "minecraft:stick", 17,
                null, "itemsadder:item/ruby", null, null, List.of());
        CustomItem archiveItem = new CustomItem("resource-pack", "minecraft:stick#17", "minecraft:stick", 17,
                null, "itemsadder:item/ruby", "itemsadder/textures/item/ruby.png", null, List.of());
        CustomItem existingNextValue = new CustomItem("resource-pack", "minecraft:stick#18", "minecraft:stick", 18,
                null, "itemsadder:item/emerald", "itemsadder/textures/item/emerald.png", null, List.of());
        MappingGenerator.Result result = new MappingGenerator().generate(List.of(apiItem, archiveItem, existingNextValue));
        JsonTest.check(result.items().size() == 3, "duplicate Java item mappings emit all Bedrock items");
        JsonTest.check("resource-pack".equals(result.items().get(0).source()),
                "resolved generated archive items are ordered before unresolved API duplicate");
        JsonTest.check(!Objects.equals(result.items().get(0).bedrockIdentifier(), result.items().get(1).bedrockIdentifier()),
                "duplicate Java mappings receive unique Bedrock identifiers");
        List<Object> entries = Json.array(Json.object(Json.object(result.json()).get("items")).get("minecraft:stick"));
        List<Integer> values = entries.stream().map(Json::object).map(entry -> ((Number) entry.get("custom_model_data")).intValue()).toList();
        JsonTest.check(values.size() == 3 && values.containsAll(List.of(17, 18, 19)),
                "duplicate CMD is reassigned without colliding with another existing value");
        JsonTest.check(result.items().stream().anyMatch(item -> item.source().equals("itemsadder") && item.customModelData() == 19),
                "the generated item carries the reassigned CMD");
        JsonTest.check(result.warnings().stream().anyMatch(warning -> warning.contains("17 -> 19")),
                "reassigned CMD is reported so the Java item can be updated");
    }

    private static void generatesNamedCustomBlockStateOverrides() {
        BedrockCustomBlock first = new BedrockCustomBlock("itemsadder", "ores:ruby", "minecraft:note_block",
                Map.of("instrument", "snare", "note", "0", "powered", "false"), "ores:textures/ruby.png", null);
        BedrockCustomBlock second = new BedrockCustomBlock("itemsadder", "ores:sapphire", "minecraft:note_block",
                Map.of("instrument", "basedrum", "note", "1", "powered", "false"), "ores:textures/sapphire.png", null);
        MappingGenerator.Result result = new MappingGenerator().generate(List.of(), List.of(first, second));
        Map<String, Object> blockRoot = Json.object(result.blockJson());
        JsonTest.check(((Number) blockRoot.get("format_version")).intValue() == 1, "block mappings use Geyser's block mapping format version");
        Map<String, Object> blockMappings = Json.object(blockRoot.get("blocks"));
        Map<String, Object> noteBlock = Json.object(blockMappings.get("minecraft:note_block"));
        JsonTest.check("smc_custom_block".equals(noteBlock.get("name")), "custom block mapping has its required top-level name");
        Map<String, Object> overrides = Json.object(noteBlock.get("state_overrides"));
        JsonTest.check(overrides.size() == 2, "all custom block states are mapped");
        JsonTest.check("smc:itemsadder_ores_ruby".equals(Json.object(overrides.get("instrument=snare,note=0,powered=false")).get("name")),
                "Geyser block name matches the generated Bedrock block identifier");
    }
}