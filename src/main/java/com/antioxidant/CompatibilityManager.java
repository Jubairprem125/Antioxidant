package com.antioxidant;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CompatibilityManager {
    private final Plugin owner;
    private volatile Path geyserPackDirectory;
    private volatile Path geyserConfigDirectory;
    private volatile String geyserVersion = "not detected";
    private volatile String floodgateStatus = "not detected";
    private volatile String itemsAdderVersion = "not detected";
    private volatile String nexoVersion = "not detected";

    public CompatibilityManager(Plugin owner) {
        this.owner = owner;
        detect();
    }

    public void detect() {
        Plugin geyser = findPlugin("Geyser-Spigot", "Geyser");
        geyserVersion = version(geyser);
        floodgateStatus = version(findPlugin("floodgate", "Floodgate"));
        itemsAdderVersion = version(findPlugin("ItemsAdder"));
        nexoVersion = version(findPlugin("Nexo"));
        geyserPackDirectory = resolveGeyserPackDirectory(geyser != null);
        geyserConfigDirectory = resolveGeyserConfigDirectory(geyser != null);
    }

    public List<CustomItem> snapshotItems(boolean scanItemsAdder, boolean scanNexo) {
        List<CustomItem> result = new ArrayList<>();
        if (scanItemsAdder && !"not detected".equals(itemsAdderVersion)) collectItemsAdder(result);
        if (scanNexo && !"not detected".equals(nexoVersion)) collectNexo(result);
        return List.copyOf(result);
    }

    public List<BedrockCustomBlock> snapshotBlocks(boolean scanItemsAdder) {
        List<BedrockCustomBlock> result = new ArrayList<>();
        if (scanItemsAdder && !"not detected".equals(itemsAdderVersion)) collectItemsAdderBlocks(result);
        return List.copyOf(result);
    }

    public boolean isBedrockPlayer(UUID uuid) {
        try {
            Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object instance = api.getMethod("api").invoke(null);
            return (boolean) api.getMethod("isBedrockPlayer", UUID.class).invoke(instance, uuid);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return false;
        }
    }

    public boolean itemsAdderLoaded() {
        if ("not detected".equals(itemsAdderVersion)) return true;
        try {
            Class<?> itemsAdder = Class.forName("dev.lone.itemsadder.api.ItemsAdder");
            return (boolean) itemsAdder.getMethod("areItemsLoaded").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            return true;
        }
    }

    public Path geyserPackDirectory() {
        return geyserPackDirectory;
    }

    public Path geyserConfigDirectory() {
        return geyserConfigDirectory;
    }

    public String geyserVersion() { return geyserVersion; }
    public String floodgateStatus() { return floodgateStatus; }
    public String itemsAdderVersion() { return itemsAdderVersion; }
    public String nexoVersion() { return nexoVersion; }

    public String platform() {
        return Bukkit.getName();
    }

    private void collectItemsAdder(List<CustomItem> target) {
        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object idsObject = customStack.getMethod("getNamespacedIdsInRegistry").invoke(null);
            if (!(idsObject instanceof Collection<?> ids)) return;
            Method getInstance = customStack.getMethod("getInstance", String.class);
            for (Object rawId : ids) {
                String id = String.valueOf(rawId);
                try {
                    Object stack = getInstance.invoke(null, id);
                    if (stack == null) continue;
                    ItemStack itemStack = (ItemStack) stack.getClass().getMethod("getItemStack").invoke(stack);
                    String model = stringMethod(stack, "getModelPath");
                    String texture = firstTexture(stack);
                    target.add(describe("itemsadder", id, itemStack, null, model, texture));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    owner.getLogger().warning("Could not inspect ItemsAdder item " + id + ": " + exception.getMessage());
                }
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            owner.getLogger().warning("ItemsAdder API scan was unavailable: " + exception.getMessage());
        }
    }

    private void collectNexo(List<CustomItem> target) {
        try {
            Class<?> nexoItems = Class.forName("com.nexomc.nexo.api.NexoItems");
            Object namesObject = nexoItems.getMethod("itemNames").invoke(null);
            if (!(namesObject instanceof Collection<?> names)) return;
            Method itemFromId = nexoItems.getMethod("itemFromId", String.class);
            for (Object rawName : names) {
                String name = String.valueOf(rawName);
                try {
                    Object builder = itemFromId.invoke(null, name);
                    if (builder == null) continue;
                    ItemStack itemStack = (ItemStack) builder.getClass().getMethod("build").invoke(builder);
                    String id = name;
                    try {
                        id = String.valueOf(nexoItems.getMethod("idFromItem", builder.getClass()).invoke(null, builder));
                    } catch (ReflectiveOperationException ignored) {
                    }
                    String model = null;
                    try {
                        Object key = builder.getClass().getMethod("getItemModel").invoke(builder);
                        if (key != null) model = key.toString();
                    } catch (ReflectiveOperationException ignored) {
                    }
                    target.add(describe("nexo", id, itemStack, model, model, null));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    owner.getLogger().warning("Could not inspect Nexo item " + name + ": " + exception.getMessage());
                }
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            owner.getLogger().warning("Nexo API scan was unavailable: " + exception.getMessage());
        }
    }

    private void collectItemsAdderBlocks(List<BedrockCustomBlock> target) {
        try {
            Class<?> customBlock = Class.forName("dev.lone.itemsadder.api.CustomBlock");
            Object idsObject = customBlock.getMethod("getNamespacedIdsInRegistry").invoke(null);
            if (!(idsObject instanceof Collection<?> ids)) return;
            Method getInstance = customBlock.getMethod("getInstance", String.class);
            for (Object rawId : ids) {
                String id = String.valueOf(rawId);
                try {
                    Object block = getInstance.invoke(null, id);
                    if (block == null) continue;
                    Object baseData = block.getClass().getMethod("getBaseBlockData").invoke(block);
                    String data = baseData == null ? null : blockDataString(baseData);
                    int stateStart = data == null ? -1 : data.indexOf('[');
                    String base = stateStart < 0 ? data : data.substring(0, stateStart);
                    Map<String, String> states = parseBlockStates(data, stateStart);
                    if (base == null || base.isBlank() || states.isEmpty()) {
                        owner.getLogger().warning("Could not read base block states for ItemsAdder block " + id + " from " + data);
                        continue;
                    }
                    String normalizedBase = base.contains(":") ? base : "minecraft:" + base;
                    String texture = firstTexture(block);
                    target.add(new BedrockCustomBlock("itemsadder", id, normalizedBase, states, texture, null));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    owner.getLogger().warning("Could not inspect ItemsAdder block " + id + ": " + exception.getMessage());
                }
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            owner.getLogger().warning("ItemsAdder custom block scan was unavailable: " + exception.getMessage());
        }
    }

    private String blockDataString(Object blockData) {
        try {
            Object value = blockData.getClass().getMethod("getAsString").invoke(blockData);
            if (value != null) return value.toString();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return blockData.toString();
    }

    private Map<String, String> parseBlockStates(String data, int stateStart) {
        if (stateStart < 0 || !data.endsWith("]")) return Map.of();
        Map<String, String> states = new LinkedHashMap<>();
        String values = data.substring(stateStart + 1, data.length() - 1);
        for (String pair : values.split(",")) {
            int separator = pair.indexOf('=');
            if (separator > 0 && separator < pair.length() - 1) {
                states.put(pair.substring(0, separator).trim(), pair.substring(separator + 1).trim());
            }
        }
        return states;
    }

    private CustomItem describe(String source, String id, ItemStack stack, String itemModel, String model, String texture) {
        if (stack == null) return new CustomItem(source, id, null, null, itemModel, model, texture, null, List.of("No item stack was returned by the integration API"));
        ItemMeta meta = stack.getItemMeta();
        Integer cmd = null;
        if (meta != null) {
            try {
                Method has = meta.getClass().getMethod("hasCustomModelData");
                if ((boolean) has.invoke(meta)) cmd = (Integer) meta.getClass().getMethod("getCustomModelData").invoke(meta);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
            if (itemModel == null) {
                try {
                    Object value = meta.getClass().getMethod("getItemModel").invoke(meta);
                    if (value != null) itemModel = value.toString();
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        }
        String base = "minecraft:" + stack.getType().name().toLowerCase(Locale.ROOT);
        return new CustomItem(source, id, base, cmd, itemModel, model, texture, null, List.of());
    }

    private String firstTexture(Object stack) {
        try {
            Object value = stack.getClass().getMethod("getTextures").invoke(stack);
            if (value instanceof List<?> list && !list.isEmpty()) return String.valueOf(list.get(0));
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private String stringMethod(Object instance, String method) {
        try {
            Object value = instance.getClass().getMethod(method).invoke(instance);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Path resolveGeyserPackDirectory(boolean detected) {
        if (!detected) return null;
        try {
            Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object instance = api.getMethod("api").invoke(null);
            Object path = api.getMethod("packDirectory").invoke(instance);
            return path instanceof Path packPath ? packPath : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private Path resolveGeyserConfigDirectory(boolean detected) {
        if (!detected) return null;
        try {
            Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object instance = api.getMethod("api").invoke(null);
            Object path = api.getMethod("configDirectory").invoke(instance);
            return path instanceof Path configPath ? configPath : null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
    }

    private Plugin findPlugin(String... names) {
        for (String name : names) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
            if (plugin != null && plugin.isEnabled()) return plugin;
        }
        return null;
    }

    private String version(Plugin plugin) {
        return plugin == null ? "not detected" : plugin.getDescription().getVersion();
    }
}