package com.antioxidant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ResourcePackScannerTest {
    public static void run() {
        Path root = null;
        try {
            root = Files.createTempDirectory("smc-pack-scan-test");
            writePack(root.resolve("pack-one"), 11, "one:item/one");
            writePack(root.resolve("pack-two"), 22, "two:item/two");
            writeText(root.resolve("pack-one/assets/one/models/item/one.json"),
                    "{\"textures\":{\"side\":\"one:item/one\"},\"elements\":[{\"faces\":{\"north\":{\"texture\":\"#side\"}}}]}");
            Files.createDirectories(root.resolve("pack-one/assets/one/textures/item"));
            Files.write(root.resolve("pack-one/assets/one/textures/item/one.png"), new byte[0]);
            writeText(root.resolve("pack-one/assets/one/models/item/bare.json"),
                    "{\"textures\":{\"layer0\":\"item/bare\"}}");
            Files.write(root.resolve("pack-one/assets/one/textures/item/bare.png"), new byte[0]);
            writeText(root.resolve("pack-one/assets/one/models/item/cross-namespace.json"),
                    "{\"textures\":{\"layer0\":\"item/crystal\"}}");
            Files.createDirectories(root.resolve("pack-two/assets/two/textures/gear"));
            Files.write(root.resolve("pack-two/assets/two/textures/gear/crystal.png"), new byte[0]);
            writeText(root.resolve("pack-one/assets/one/models/item/fallback.json"),
                    "{\"textures\":{\"particle\":\"missing:item/texture\",\"custom\":\"one:item/fallback\"}}");
            Files.write(root.resolve("pack-one/assets/one/textures/item/fallback.png"), new byte[0]);
            ResourcePackScanner.PackIndex index = new ResourcePackScanner().scan(
                    List.of(root.resolve("pack-one"), root.resolve("pack-two")), message -> {});
            List<String> identifiers = index.discoveredItems().stream().map(CustomItem::javaIdentifier).toList();
            JsonTest.check(identifiers.size() == 2, "item overrides from both packs are discovered");
            JsonTest.check(identifiers.contains("minecraft:stick#11") && identifiers.contains("minecraft:stick#22"),
                    "overrides sharing the same base model are retained");
            CustomItem blockModelItem = index.discoveredItems().stream()
                    .filter(item -> item.javaIdentifier().endsWith("#11")).findFirst().orElseThrow();
            JsonTest.check("one/textures/item/one.png".equals(index.resolve(blockModelItem).texture()),
                    "textures referenced by 3D model faces are resolved for item icons");
            CustomItem namespaceRelativeItem = new CustomItem("resource-pack", "minecraft:stick#34", "minecraft:stick",
                    34, null, "one:item/bare", null, null, List.of());
            JsonTest.check("one/textures/item/bare.png".equals(index.resolve(namespaceRelativeItem).texture()),
                    "bare model texture references resolve in the model namespace");
            CustomItem crossNamespaceItem = new CustomItem("resource-pack", "minecraft:stick#35", "minecraft:stick",
                    35, null, "one:item/cross-namespace", null, null, List.of());
            JsonTest.check("two/textures/gear/crystal.png".equals(index.resolve(crossNamespaceItem).texture()),
                    "a uniquely named texture resolves when its model is stored under another namespace");
            CustomItem fallbackTextureItem = new CustomItem("resource-pack", "minecraft:stick#33", "minecraft:stick",
                    33, null, "one:item/fallback", null, null, List.of());
            JsonTest.check("one/textures/item/fallback.png".equals(index.resolve(fallbackTextureItem).texture()),
                    "custom model texture keys are tried when preferred texture references are missing");

            Path generatedZip = root.resolve("ItemsAdder/output/generated.zip");
            writeGeneratedZip(generatedZip);
            ResourcePackScanner.PackIndex generatedIndex = new ResourcePackScanner().scan(List.of(generatedZip), message -> {});
            JsonTest.check(generatedIndex.discoveredItems().size() == 2,
                    "all custom item overrides inside ItemsAdder generated.zip are discovered");
            JsonTest.check("itemsadder/textures/item/crystal.png".equals(generatedIndex.resolve(
                            generatedIndex.discoveredItems().get(0)).texture())
                            || "itemsadder/textures/item/crystal.png".equals(generatedIndex.resolve(
                            generatedIndex.discoveredItems().get(1)).texture()),
                    "wrapped assets from ItemsAdder generated.zip resolve item textures");
        } catch (IOException exception) {
            throw new AssertionError("could not create resource-pack scanner test data", exception);
        } finally {
            if (root != null) {
                try (var paths = Files.walk(root)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new AssertionError("could not clean resource-pack scanner test data", exception);
                }
            }
        }
    }

    private static void writePack(Path pack, int customModelData, String model) throws IOException {
        Path baseModel = pack.resolve("assets/minecraft/models/item/stick.json");
        Files.createDirectories(baseModel.getParent());
        Files.writeString(baseModel, "{\"overrides\":[{\"predicate\":{\"custom_model_data\":"
                + customModelData + "},\"model\":\"" + model + "\"}]}");
    }

    private static void writeText(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private static void writeGeneratedZip(Path archive) throws IOException {
        Files.createDirectories(archive.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            addZipEntry(zip, "generated/assets/minecraft/models/item/stone.json",
                    "{\"overrides\":[{\"predicate\":{\"custom_model_data\":77},\"model\":\"itemsadder:item/crystal\"},"
                            + "{\"predicate\":{\"custom_model_data\":88},\"model\":\"itemsadder:item/crystal\"}]}");
            addZipEntry(zip, "generated/assets/itemsadder/models/item/crystal.json",
                    "{\"textures\":{\"layer0\":\"itemsadder:item/crystal\"}}");
            addZipEntry(zip, "generated/assets/itemsadder/textures/item/crystal.png", "texture".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private static void addZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        addZipEntry(zip, name, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void addZipEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }
}