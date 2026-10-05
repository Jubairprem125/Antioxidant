package com.antioxidant;

import java.util.Map;
import java.util.TreeMap;

public record BedrockCustomBlock(
        String source,
        String javaIdentifier,
        String javaBaseBlock,
        Map<String, String> javaStates,
        String texture,
        String bedrockIdentifier
) {
    public BedrockCustomBlock {
        javaStates = javaStates == null ? Map.of() : Map.copyOf(new TreeMap<>(javaStates));
    }

    public BedrockCustomBlock withResolvedTexture(String resolvedTexture) {
        return new BedrockCustomBlock(source, javaIdentifier, javaBaseBlock, javaStates,
                resolvedTexture, bedrockIdentifier);
    }

    public BedrockCustomBlock withBedrockIdentifier(String identifier) {
        return new BedrockCustomBlock(source, javaIdentifier, javaBaseBlock, javaStates,
                texture, identifier);
    }

    public String stableKey() {
        return source + '|' + javaIdentifier + '|' + javaBaseBlock + '|' + javaStates;
    }
}