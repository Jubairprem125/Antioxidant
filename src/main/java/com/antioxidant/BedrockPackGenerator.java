package com.antioxidant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BedrockPackGenerator {
    public PackResult generate(Path pluginFolder, Path output, List<CustomItem> items, List<BedrockCustomBlock> blocks,
                               ResourcePackScanner.PackIndex assets, HashCache cache,
                               boolean generatePack) throws IOException {
        Files.createDirectories(output);
        Path packRoot = output.resolve("pack");
        Files.createDirectories(packRoot);
        Path behaviorPackRoot = output.resolve("behavior-pack");
        Files.createDirectories(behaviorPackRoot);
        UUID headerUuid = stablePackUuid(pluginFolder);
        UUID moduleUuid = UUID.nameUUIDFromBytes((headerUuid + ":module").getBytes(StandardCharsets.UTF_8));
        UUID behaviorHeaderUuid = UUID.nameUUIDFromBytes((headerUuid + ":behavior-header").getBytes(StandardCharsets.UTF_8));
        UUID behaviorModuleUuid = UUID.nameUUIDFromBytes((headerUuid + ":behavior-module").getBytes(StandardCharsets.UTF_8));
        Map<String, Object> resourceManifest = new LinkedHashMap<>();
        resourceManifest.put("format_version", 2);
        resourceManifest.put("header", Map.of("name", "Antioxidant Bedrock Resources", "description", "Generated from Java resource-pack item data", "uuid", headerUuid.toString(), "version", List.of(1, 0, 0), "min_engine_version", List.of(1, 20, 0)));
        resourceManifest.put("modules", List.of(Map.of("type", "resources", "uuid", moduleUuid.toString(), "version", List.of(1, 0, 0))));
        cache.writeIfChanged("pack/manifest.json", packRoot.resolve("manifest.json"), Json.stringify(resourceManifest).getBytes(StandardCharsets.UTF_8));
        Map<String, Object> behaviorManifest = new LinkedHashMap<>();
        behaviorManifest.put("format_version", 2);
        behaviorManifest.put("header", Map.of("name", "Antioxidant Bedrock Items", "description", "Generated item definitions", "uuid", behaviorHeaderUuid.toString(), "version", List.of(1, 0, 0), "min_engine_version", List.of(1, 20, 0)));
        behaviorManifest.put("modules", List.of(Map.of("type", "data", "uuid", behaviorModuleUuid.toString(), "version", List.of(1, 0, 0))));
        behaviorManifest.put("dependencies", List.of(Map.of("uuid", headerUuid.toString(), "version", List.of(1, 0, 0))));
        cache.writeIfChanged("behavior-pack/manifest.json", behaviorPackRoot.resolve("manifest.json"), Json.stringify(behaviorManifest).getBytes(StandardCharsets.UTF_8));

        Map<String, Object> textureData = new LinkedHashMap<>();
        Map<String, Object> terrainTextureData = new LinkedHashMap<>();
        Map<String, Object> generatedItems = new LinkedHashMap<>();
        Map<String, Object> generatedAttachables = new LinkedHashMap<>();
        Map<String, Object> generatedGeometries = new LinkedHashMap<>();
        Map<String, Object> generatedBlocks = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        int textureCount = 0;
        int modelCount = 0;
        java.util.Set<String> blockIdentifiers = blocks.stream().map(BedrockCustomBlock::bedrockIdentifier)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        for (CustomItem item : items) {
            String id = item.bedrockIdentifier();
            if (id == null) continue;
            String textureKey = MappingGenerator.geyserIconKey(id);
            String targetTexture = "textures/items/" + textureKey + ".png";
            byte[] png = null;
            String textureReadError = null;
            try {
                if (item.texture() != null && !item.texture().isBlank()) png = assets.readTexture(item.texture());
            } catch (IOException | RuntimeException exception) {
                textureReadError = exception.getMessage();
            }
            if (png == null) {
                if (textureReadError != null) warnings.add("Could not read texture for " + item.javaIdentifier() + ": " + textureReadError);
                else if (item.texture() == null || item.texture().isBlank()) warnings.add("No texture could be resolved for " + item.javaIdentifier());
                else warnings.add("Missing texture " + item.texture() + " for " + item.javaIdentifier());
                textureData.put(textureKey, Map.of("textures", "textures/items/paper"));
            } else {
                cache.writeIfChanged("pack/" + targetTexture, packRoot.resolve(targetTexture), png);
                textureCount++;
                textureData.put(textureKey, Map.of("textures", "textures/items/" + textureKey));
            }
            Map<String, Object> description = new LinkedHashMap<>();
            description.put("identifier", id);
            description.put("menu_category", Map.of("category", "items"));
            Map<String, Object> components = new LinkedHashMap<>();
            components.put("minecraft:icon", Map.of("texture", textureKey));
            components.put("minecraft:display_name", Map.of("value", displayName(item.javaIdentifier())));
            if (blockIdentifiers.contains(id)) components.put("minecraft:block_placer", Map.of("block", id));
            generatedItems.put(textureKey + ".json", Map.of("format_version", "1.20.50", "minecraft:item", Map.of("description", description, "components", components)));
            if (png != null && item.model() != null) {
                try {
                    Map<String, Object> geometry = itemGeometry(item, textureKey, assets, warnings);
                    if (geometry != null) {
                        String geometryId = "geometry.smc." + MappingGenerator.slug(id.replace(':', '_'));
                        geometry.put("description", geometryDescription(geometryId));
                        generatedGeometries.put(textureKey + ".geo.json", Map.of("format_version", "1.12.0", "minecraft:geometry", List.of(geometry)));
                        generatedAttachables.put(textureKey + ".json", attachable(id, textureKey, geometryId));
                        modelCount++;
                    }
                } catch (RuntimeException exception) {
                    warnings.add("Could not convert " + item.javaIdentifier() + ": " + exception.getMessage());
                }
            }
        }

        for (BedrockCustomBlock block : blocks) {
            String id = block.bedrockIdentifier();
            if (id == null || block.texture() == null) continue;
            String textureKey = MappingGenerator.geyserIconKey(id);
            try {
                byte[] png = assets.readTexture(block.texture());
                if (png == null) {
                    warnings.add("Missing block texture " + block.texture() + " for " + block.javaIdentifier());
                    continue;
                }
                String targetTexture = "textures/blocks/" + textureKey + ".png";
                cache.writeIfChanged("pack/" + targetTexture, packRoot.resolve(targetTexture), png);
                terrainTextureData.put(textureKey, Map.of("textures", targetTexture.substring(0, targetTexture.length() - 4)));
                Map<String, Object> description = Map.of("identifier", id, "menu_category", Map.of("category", "nature"));
                Map<String, Object> components = new LinkedHashMap<>();
                components.put("minecraft:geometry", "minecraft:geometry.full_block");
                components.put("minecraft:material_instances", Map.of("*", Map.of("texture", textureKey, "render_method", "opaque")));
                generatedBlocks.put(textureKey + ".json", Map.of("format_version", "1.20.50", "minecraft:block", Map.of("description", description, "components", components)));
            } catch (IOException | RuntimeException exception) {
                warnings.add("Could not convert block " + block.javaIdentifier() + ": " + exception.getMessage());
            }
        }

        Map<String, Object> textureAtlas = new LinkedHashMap<>();
        textureAtlas.put("resource_pack_name", "Antioxidant");
        textureAtlas.put("texture_name", "atlas.items");
        textureAtlas.put("texture_data", textureData);
        cache.writeIfChanged("pack/item_texture.json", packRoot.resolve("textures/item_texture.json"), Json.stringify(textureAtlas).getBytes(StandardCharsets.UTF_8));
        Map<String, Object> terrainAtlas = new LinkedHashMap<>();
        terrainAtlas.put("resource_pack_name", "Antioxidant");
        terrainAtlas.put("texture_name", "atlas.terrain");
        terrainAtlas.put("texture_data", terrainTextureData);
        cache.writeIfChanged("pack/terrain_texture.json", packRoot.resolve("textures/terrain_texture.json"), Json.stringify(terrainAtlas).getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(behaviorPackRoot.resolve("items"));
        Files.createDirectories(behaviorPackRoot.resolve("blocks"));
        Files.createDirectories(packRoot.resolve("attachables"));
        Files.createDirectories(packRoot.resolve("models/entity"));
        for (Map.Entry<String, Object> entry : generatedItems.entrySet()) {
            cache.writeIfChanged("behavior-pack/items/" + entry.getKey(), behaviorPackRoot.resolve("items").resolve(entry.getKey()), Json.stringify(entry.getValue()).getBytes(StandardCharsets.UTF_8));
        }
        for (Map.Entry<String, Object> entry : generatedBlocks.entrySet()) {
            cache.writeIfChanged("behavior-pack/blocks/" + entry.getKey(), behaviorPackRoot.resolve("blocks").resolve(entry.getKey()), Json.stringify(entry.getValue()).getBytes(StandardCharsets.UTF_8));
        }
        for (Map.Entry<String, Object> entry : generatedAttachables.entrySet()) {
            cache.writeIfChanged("pack/attachables/" + entry.getKey(), packRoot.resolve("attachables").resolve(entry.getKey()), Json.stringify(entry.getValue()).getBytes(StandardCharsets.UTF_8));
        }
        for (Map.Entry<String, Object> entry : generatedGeometries.entrySet()) {
            cache.writeIfChanged("pack/models/entity/" + entry.getKey(), packRoot.resolve("models/entity").resolve(entry.getKey()), Json.stringify(entry.getValue()).getBytes(StandardCharsets.UTF_8));
        }
        Files.createDirectories(packRoot.resolve("texts"));
        cache.writeIfChanged("pack/texts/languages.json", packRoot.resolve("texts/languages.json"), "[\"en_US\"]\n".getBytes(StandardCharsets.UTF_8));
        cache.writeIfChanged("pack/texts/en_US.lang", packRoot.resolve("texts/en_US.lang"), "".getBytes(StandardCharsets.UTF_8));

        Path resourcePackArchive = output.resolve("antioxidant-resource-pack.mcpack");
        Path behaviorPackArchive = output.resolve("antioxidant-behavior-pack.mcpack");
        Path archive = output.resolve("antioxidant-pack.mcaddon");
        if (generatePack) {
            createArchive(packRoot, resourcePackArchive);
            createArchive(behaviorPackRoot, behaviorPackArchive);
            createAddonArchive(resourcePackArchive, behaviorPackArchive, archive);
        }
        cache.save();
        return new PackResult(textureCount, modelCount, warnings, archive, resourcePackArchive, behaviorPackArchive, headerUuid.toString());
    }

    private UUID stablePackUuid(Path pluginFolder) throws IOException {
        Path uuidFile = pluginFolder.resolve("generated/pack-uuid.txt");
        Files.createDirectories(uuidFile.getParent());
        if (Files.isRegularFile(uuidFile)) {
            try {
                return UUID.fromString(Files.readString(uuidFile).trim());
            } catch (IllegalArgumentException ignored) {
            }
        }
        UUID uuid = UUID.randomUUID();
        Files.writeString(uuidFile, uuid.toString(), StandardCharsets.UTF_8);
        return uuid;
    }

    private void createArchive(Path packRoot, Path archive) throws IOException {
        Path temporary = archive.resolveSibling(archive.getFileName() + ".tmp");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
            try (var paths = Files.walk(packRoot)) {
                for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                    String name = packRoot.relativize(file).toString().replace('\\', '/');
                    zip.putNextEntry(new ZipEntry(name));
                    Files.copy(file, zip);
                    zip.closeEntry();
                }
            }
        }
        Files.move(temporary, archive, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private void createAddonArchive(Path resourcePackArchive, Path behaviorPackArchive, Path archive) throws IOException {
        Path temporary = archive.resolveSibling(archive.getFileName() + ".tmp");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
            for (Path pack : List.of(resourcePackArchive, behaviorPackArchive)) {
                zip.putNextEntry(new ZipEntry(pack.getFileName().toString()));
                Files.copy(pack, zip);
                zip.closeEntry();
            }
        }
        Files.move(temporary, archive, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    }

    private String displayName(String identifier) {
        String value = identifier == null ? "Custom Item" : identifier.substring(identifier.lastIndexOf(':') + 1).replace('_', ' ');
        return value.isBlank() ? "Custom Item" : value;
    }

    private Map<String, Object> itemGeometry(CustomItem item, String textureKey,
                                             ResourcePackScanner.PackIndex assets, List<String> warnings) {
        List<Object> elements = assets.modelElements(item.model());
        if (elements.isEmpty()) {
            warnings.add("3D model not converted for " + item.javaIdentifier() + ": no cuboid elements were found; Bedrock will use the 2D icon");
            return null;
        }
        List<Object> cubes = new ArrayList<>();
        for (Object value : elements) {
            Map<String, Object> element = Json.object(value);
            List<Object> from = Json.array(element.get("from"));
            List<Object> to = Json.array(element.get("to"));
            if (from.size() != 3 || to.size() != 3 || element.get("rotation") != null) {
                warnings.add("3D model not converted for " + item.javaIdentifier() + ": only unrotated cuboid elements are supported");
                return null;
            }
            double x1 = number(from.get(0));
            double y1 = number(from.get(1));
            double z1 = number(from.get(2));
            double x2 = number(to.get(0));
            double y2 = number(to.get(1));
            double z2 = number(to.get(2));
            Map<String, Object> cube = new LinkedHashMap<>();
            cube.put("origin", List.of(x1 - 8, y1 - 8, z1 - 8));
            cube.put("size", List.of(x2 - x1, y2 - y1, z2 - z1));
            Map<String, Object> uv = new LinkedHashMap<>();
            Json.object(element.get("faces")).forEach((face, faceValue) -> {
                List<Object> faceUv = Json.array(Json.object(faceValue).get("uv"));
                if (faceUv.size() == 4) {
                    double u1 = number(faceUv.get(0));
                    double v1 = number(faceUv.get(1));
                    double u2 = number(faceUv.get(2));
                    double v2 = number(faceUv.get(3));
                    uv.put(face, Map.of("uv", List.of(u1, v1), "uv_size", List.of(u2 - u1, v2 - v1)));
                }
            });
            if (uv.isEmpty()) cube.put("uv", List.of(0, 0));
            else cube.put("uv", uv);
            cubes.add(cube);
        }
        if (cubes.isEmpty()) {
            warnings.add("3D model not converted for " + item.javaIdentifier() + ": no usable cuboids were found");
            return null;
        }
        return new LinkedHashMap<>(Map.of("bones", List.of(
                Map.of("name", "root", "pivot", List.of(0, 0, 0)),
                Map.of("name", "rightitem", "parent", "root", "pivot", List.of(0, 0, 0), "cubes", cubes))));
    }

    private Map<String, Object> geometryDescription(String identifier) {
        return Map.of("identifier", identifier, "texture_width", 16, "texture_height", 16,
                "visible_bounds_width", 2, "visible_bounds_height", 2, "visible_bounds_offset", List.of(0, 0, 0));
    }

    private Map<String, Object> attachable(String identifier, String textureKey, String geometryId) {
        Map<String, Object> description = new LinkedHashMap<>();
        description.put("identifier", identifier);
        description.put("materials", Map.of("default", "entity_alphatest"));
        description.put("textures", Map.of("default", "textures/items/" + textureKey));
        description.put("geometry", Map.of("default", geometryId));
        description.put("render_controllers", List.of("controller.render.item_default"));
        return Map.of("format_version", "1.10.0", "minecraft:attachable", Map.of("description", description));
    }

    private double number(Object value) {
        if (!(value instanceof Number number)) throw new IllegalArgumentException("Model coordinates must be numeric");
        return number.doubleValue();
    }

    public record PackResult(int textures, int models, List<String> warnings, Path archive,
                             Path resourcePackArchive, Path behaviorPackArchive, String packUuid) {}
}