package com.antioxidant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

public final class ResourcePackScanner {
    public PackIndex scan(List<Path> roots, Consumer<String> warningSink) {
        Map<String, AssetRef> assets = new LinkedHashMap<>();
        Map<String, List<AssetRef>> itemModelSources = new LinkedHashMap<>();
        Set<Path> visitedAssetsDirectories = new HashSet<>();
        for (Path root : roots) {
            if (!Files.exists(root)) continue;
            try {
                if (Files.isRegularFile(root) && root.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip")) {
                    indexZip(root, assets, itemModelSources, warningSink);
                } else if (Files.isDirectory(root)) {
                    try (var paths = Files.walk(root)) {
                        List<Path> all = paths.toList();
                        for (Path path : all) {
                            if (Files.isDirectory(path) && path.getFileName() != null && path.getFileName().toString().equals("assets")) {
                                Path normalized = path.toAbsolutePath().normalize();
                                if (visitedAssetsDirectories.add(normalized)) indexDirectory(path, assets, itemModelSources, warningSink);
                            } else if (Files.isRegularFile(path) && path.toString().toLowerCase().endsWith(".zip")) {
                                indexZip(path, assets, itemModelSources, warningSink);
                            }
                        }
                    }
                }
            } catch (IOException exception) {
                warningSink.accept("Could not scan " + root + ": " + exception.getMessage());
            }
        }
        return new PackIndex(assets, itemModelSources, warningSink);
    }

    private void indexDirectory(Path assetsRoot, Map<String, AssetRef> assets,
                                Map<String, List<AssetRef>> itemModelSources, Consumer<String> warningSink) throws IOException {
        try (var paths = Files.walk(assetsRoot)) {
            for (Path file : paths.filter(Files::isRegularFile).toList()) {
                String relative = assetsRoot.relativize(file).toString().replace('\\', '/');
                if (isUseful(relative)) {
                    AssetRef ref = new AssetRef(file, null, null);
                    assets.put(relative, ref);
                    if (isItemModel(relative)) itemModelSources.computeIfAbsent(relative, ignored -> new ArrayList<>()).add(ref);
                }
            }
        } catch (IOException exception) {
            warningSink.accept("Could not read resource assets " + assetsRoot + ": " + exception.getMessage());
        }
    }

    private void indexZip(Path archive, Map<String, AssetRef> assets,
                          Map<String, List<AssetRef>> itemModelSources, Consumer<String> warningSink) {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String entryName = entry.getName().replace('\\', '/');
                int assetsIndex = entryName.indexOf("assets/");
                if (assetsIndex < 0 || (assetsIndex > 0 && entryName.charAt(assetsIndex - 1) != '/')) continue;
                String relative = entryName.substring(assetsIndex + "assets/".length());
                if (isUseful(relative)) {
                    AssetRef ref = new AssetRef(null, archive, entry.getName());
                    assets.put(relative, ref);
                    if (isItemModel(relative)) itemModelSources.computeIfAbsent(relative, ignored -> new ArrayList<>()).add(ref);
                }
            }
        } catch (IOException exception) {
            warningSink.accept("Could not read resource-pack archive " + archive + ": " + exception.getMessage());
        }
    }

    private boolean isItemModel(String relative) {
        return relative.startsWith("minecraft/models/item/") && relative.endsWith(".json");
    }

    private boolean isUseful(String relative) {
        return relative.endsWith(".json") || relative.endsWith(".png");
    }

    public record AssetRef(Path file, Path archive, String entry) {
        public byte[] read() throws IOException {
            if (file != null) return Files.readAllBytes(file);
            try (ZipFile zip = new ZipFile(archive.toFile()); var input = zip.getInputStream(zip.getEntry(entry))) {
                return input.readAllBytes();
            }
        }
    }

    public static final class PackIndex {
        private final Map<String, AssetRef> assets;
        private final Consumer<String> warningSink;
        private final List<CustomItem> discovered = new ArrayList<>();
        private final Map<String, Map<String, Object>> modelCache = new HashMap<>();

        private PackIndex(Map<String, AssetRef> assets, Map<String, List<AssetRef>> itemModelSources,
                          Consumer<String> warningSink) {
            this.assets = assets;
            this.warningSink = warningSink;
            discoverOverrides(itemModelSources);
            discoverItemDefinitions();
        }

        public Map<String, AssetRef> assets() {
            return assets;
        }

        public List<CustomItem> discoveredItems() {
            return List.copyOf(discovered);
        }

        public CustomItem resolve(CustomItem item) {
            String model = normalizeModel(item.model());
            String texture = normalizeTexture(item.texture());
            if (texture == null || !assets.containsKey(texture)) texture = textureForModel(model);
            return item.withResolved(model, texture);
        }

        public BedrockCustomBlock resolve(BedrockCustomBlock block) {
            String texture = normalizeTexture(block.texture());
            return block.withResolvedTexture(texture != null && assets.containsKey(texture) ? texture : null);
        }

        public byte[] readTexture(String texturePath) throws IOException {
            AssetRef ref = assets.get(normalizeTexture(texturePath));
            return ref == null ? null : ref.read();
        }

        public List<Object> modelElements(String modelRef) {
            return modelElements(normalizeModel(modelRef), new HashSet<>(), 0);
        }

        private List<Object> modelElements(String modelPath, Set<String> visited, int depth) {
            if (modelPath == null || depth > 16 || !visited.add(modelPath)) return List.of();
            Map<String, Object> data = model(modelPath);
            List<Object> elements = Json.array(data.get("elements"));
            if (!elements.isEmpty()) return elements;
            String parent = string(data.get("parent"));
            return parent == null ? List.of() : modelElements(normalizeModel(parent), visited, depth + 1);
        }

        private void discoverOverrides(Map<String, List<AssetRef>> itemModelSources) {
            itemModelSources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                String path = entry.getKey();
                String base = path.substring("minecraft/models/item/".length(), path.length() - ".json".length());
                String baseItem = "minecraft:" + base.substring(base.lastIndexOf('/') + 1);
                for (AssetRef source : entry.getValue()) {
                    try {
                        Map<String, Object> root = parseJson(source, path);
                        for (Object overrideValue : Json.array(root.get("overrides"))) {
                            Map<String, Object> override = Json.object(overrideValue);
                            Object raw = Json.object(override.get("predicate")).get("custom_model_data");
                            Integer cmd = integer(raw);
                            String modelRef = string(override.get("model"));
                            if (cmd == null || modelRef == null) continue;
                            discovered.add(new CustomItem("resource-pack", baseItem + "#" + cmd, baseItem,
                                    cmd, null, modelRef, null, null, List.of()));
                        }
                    } catch (RuntimeException exception) {
                        warningSink.accept("Invalid model " + path + ": " + exception.getMessage());
                    }
                }
            });
        }

        private void discoverItemDefinitions() {
            assets.keySet().stream().filter(path -> path.matches("[^/]+/items/.+\\.json")).sorted().forEach(path -> {
                try {
                    Map<String, Object> definition = json(path);
                    String baseItem = string(definition.get("base_item"));
                    String itemModel = string(definition.get("item_model"));
                    Map<String, Object> modelObj = Json.object(definition.get("model"));
                    String model = string(modelObj.get("model"));
                    if (model == null) model = string(definition.get("model"));
                    if (baseItem == null || (model == null && itemModel == null)) return;
                    String javaId = path.substring(0, path.length() - ".json".length()).replace("/items/", ":");
                    discovered.add(new CustomItem("resource-pack", javaId, baseItem, null, itemModel,
                            model, null, null, List.of()));
                } catch (RuntimeException exception) {
                    warningSink.accept("Invalid item definition " + path + ": " + exception.getMessage());
                }
            });
        }

        private String textureForModel(String modelRef) {
            if (modelRef == null || modelRef.isBlank()) return null;
            return textureForModel(normalizeModel(modelRef), new HashSet<>(), 0);
        }

        private String textureForModel(String modelPath, Set<String> visited, int depth) {
            if (modelPath == null || depth > 16 || !visited.add(modelPath)) return null;
            try {
                Map<String, Object> data = model(modelPath);
                Map<String, Object> textures = Json.object(data.get("textures"));
                List<String> references = new ArrayList<>();
                for (String key : List.of("layer0", "layer1")) {
                    String reference = string(textures.get(key));
                    if (reference != null) references.add(reference);
                }
                for (Object elementValue : Json.array(data.get("elements"))) {
                    Map<String, Object> faces = Json.object(Json.object(elementValue).get("faces"));
                    for (Object faceValue : faces.values()) {
                        String reference = string(Json.object(faceValue).get("texture"));
                        if (reference != null) references.add(reference);
                    }
                }
                for (String key : List.of("all", "particle")) {
                    String reference = string(textures.get(key));
                    if (reference != null) references.add(reference);
                }
                textures.entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .map(entry -> string(entry.getValue())).filter(java.util.Objects::nonNull)
                        .forEach(references::add);
                for (String reference : references) {
                    String resolved = resolveTextureReference(modelPath, reference, new HashSet<>(), 0);
                    if (resolved != null) return resolved;
                }
                String parent = string(data.get("parent"));
                return parent == null ? null : textureForModel(normalizeModel(parent), visited, depth + 1);
            } catch (RuntimeException exception) {
                warningSink.accept("Could not resolve model " + modelPath + ": " + exception.getMessage());
                return null;
            }
        }

        private String resolveTextureReference(String modelPath, String reference, Set<String> visitedVariables, int depth) {
            if (reference == null || depth > 16) return null;
            if (!reference.startsWith("#")) {
                String textureReference = reference;
                if (!reference.contains(":") && !reference.startsWith("assets/") && !reference.contains("/assets/")) {
                    int namespaceEnd = modelPath.indexOf('/');
                    if (namespaceEnd > 0) textureReference = modelPath.substring(0, namespaceEnd) + ":" + reference;
                }
                String resolved = normalizeTexture(textureReference);
                if (assets.containsKey(resolved)) return resolved;
                int namespaceEnd = resolved.indexOf('/');
                if (namespaceEnd < 0) return null;
                String namespace = resolved.substring(0, namespaceEnd);
                String fileName = resolved.substring(resolved.lastIndexOf('/') + 1);
                List<String> matchingTextures = assets.keySet().stream()
                        .filter(path -> path.startsWith(namespace + "/textures/") && path.endsWith("/" + fileName))
                        .toList();
                if (matchingTextures.size() == 1) return matchingTextures.get(0);
                if (!matchingTextures.isEmpty()) return null;
                List<String> crossNamespaceMatches = assets.keySet().stream()
                        .filter(path -> path.matches("[^/]+/textures/.+") && path.endsWith("/" + fileName))
                        .toList();
                return crossNamespaceMatches.size() == 1 ? crossNamespaceMatches.get(0) : null;
            }
            String key = reference.substring(1);
            if (!visitedVariables.add(key)) return null;
            String currentPath = modelPath;
            Set<String> visitedModels = new HashSet<>();
            while (currentPath != null && visitedModels.add(currentPath)) {
                Map<String, Object> data = model(currentPath);
                String value = string(Json.object(data.get("textures")).get(key));
                if (value != null) return resolveTextureReference(currentPath, value, visitedVariables, depth + 1);
                String parent = string(data.get("parent"));
                currentPath = parent == null ? null : normalizeModel(parent);
            }
            return null;
        }

        private Map<String, Object> model(String assetPath) {
            return modelCache.computeIfAbsent(assetPath, this::loadJson);
        }

        private Map<String, Object> json(String assetPath) {
            return loadJson(assetPath);
        }

        private Map<String, Object> loadJson(String assetPath) {
            AssetRef ref = assets.get(assetPath);
            if (ref == null) return Map.of();
            return parseJson(ref, assetPath);
        }

        private Map<String, Object> parseJson(AssetRef ref, String assetPath) {
            try {
                return Json.object(Json.parse(new String(ref.read(), java.nio.charset.StandardCharsets.UTF_8)));
            } catch (IOException | IllegalArgumentException exception) {
                warningSink.accept("Could not parse " + assetPath + ": " + exception.getMessage());
                return Map.of();
            }
        }

        private static String normalizeModel(String model) {
            if (model == null || model.isBlank()) return null;
            String value = model.replace("\\", "/");
            int assetsIndex = value.lastIndexOf("/assets/");
            if (assetsIndex >= 0) value = value.substring(assetsIndex + "/assets/".length());
            else if (value.startsWith("assets/")) value = value.substring("assets/".length());
            int colon = value.indexOf(':');
            String namespace;
            String path;
            if (colon >= 0) {
                namespace = value.substring(0, colon);
                path = value.substring(colon + 1);
            } else if (value.matches("[^/]+/models/.+")) {
                int namespaceEnd = value.indexOf('/');
                namespace = value.substring(0, namespaceEnd);
                path = value.substring(namespaceEnd + 1);
            } else {
                namespace = "minecraft";
                path = value;
            }
            if (path.startsWith("models/")) path = path.substring("models/".length());
            if (path.endsWith(".json")) path = path.substring(0, path.length() - 5);
            return namespace + "/models/" + path + ".json";
        }

        private static String normalizeTexture(String texture) {
            if (texture == null || texture.isBlank()) return null;
            String value = texture.replace("\\", "/");
            int assetsIndex = value.lastIndexOf("/assets/");
            if (assetsIndex >= 0) value = value.substring(assetsIndex + "/assets/".length());
            else if (value.startsWith("assets/")) value = value.substring("assets/".length());
            int colon = value.indexOf(':');
            String namespace;
            String path;
            if (colon >= 0) {
                namespace = value.substring(0, colon);
                path = value.substring(colon + 1);
            } else if (value.matches("[^/]+/textures/.+")) {
                int namespaceEnd = value.indexOf('/');
                namespace = value.substring(0, namespaceEnd);
                path = value.substring(namespaceEnd + 1);
            } else {
                namespace = "minecraft";
                path = value;
            }
            if (path.startsWith(namespace + "/textures/")) return path;
            if (path.startsWith("textures/")) return namespace + "/" + path + (path.endsWith(".png") ? "" : ".png");
            if (path.endsWith(".png")) path = path.substring(0, path.length() - 4);
            return namespace + "/textures/" + path + ".png";
        }

        private static Integer integer(Object value) {
            if (!(value instanceof Number number)) return null;
            double doubleValue = number.doubleValue();
            if (doubleValue < 0 || doubleValue > Integer.MAX_VALUE || doubleValue != Math.rint(doubleValue)) return null;
            return (int) doubleValue;
        }

        private static String string(Object value) {
            return value instanceof String string && !string.isBlank() ? string : null;
        }
    }
}