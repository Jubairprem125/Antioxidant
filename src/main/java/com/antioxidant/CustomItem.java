package com.antioxidant;

import java.util.List;

public record CustomItem(
        String source,
        String javaIdentifier,
        String baseItem,
        Integer customModelData,
        String itemModel,
        String model,
        String texture,
        String bedrockIdentifier,
        List<String> warnings
) {
    public CustomItem {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public CustomItem withResolved(String resolvedModel, String resolvedTexture) {
        return new CustomItem(source, javaIdentifier, baseItem, customModelData, itemModel,
                resolvedModel, resolvedTexture, bedrockIdentifier, warnings);
    }

    public CustomItem withBedrockIdentifier(String identifier) {
        return new CustomItem(source, javaIdentifier, baseItem, customModelData, itemModel,
                model, texture, identifier, warnings);
    }

    public CustomItem withCustomModelData(Integer value) {
        return new CustomItem(source, javaIdentifier, baseItem, value, itemModel,
                model, texture, bedrockIdentifier, warnings);
    }

    public String stableKey() {
        return source + '|' + javaIdentifier + '|' + baseItem + '|' + customModelData + '|' + itemModel + '|' + model;
    }
}