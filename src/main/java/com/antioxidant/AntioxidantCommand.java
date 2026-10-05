package com.antioxidant;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class AntioxidantCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("reload", "scan", "generate", "status", "debug", "mappings");
    private final JavaPlugin plugin;
    private final ConversionService service;
    private final CompatibilityManager compatibility;

    public AntioxidantCommand(JavaPlugin plugin, ConversionService service, CompatibilityManager compatibility) {
        this.plugin = plugin;
        this.service = service;
        this.compatibility = compatibility;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antioxidant.admin")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }
        String action = args.length == 0 ? "status" : args[0].toLowerCase(java.util.Locale.ROOT);
        switch (action) {
            case "reload" -> {
                plugin.reloadConfig();
                compatibility.detect();
                start(sender);
            }
            case "scan", "generate" -> start(sender);
            case "status" -> sendStatus(sender, false);
            case "debug" -> sendStatus(sender, true);
            case "mappings" -> sender.sendMessage("Mappings: " + plugin.getDataFolder().toPath().resolve("generated/mappings/antioxidant-mappings.json"));
            default -> sender.sendMessage("Usage: /antioxidant [reload|scan|generate|status|debug|mappings]");
        }
        return true;
    }

    private void start(CommandSender sender) {
        boolean started = service.start(plugin.getConfig().getBoolean("itemsadder.enabled", true),
                plugin.getConfig().getBoolean("nexo.enabled", true),
                () -> sender.sendMessage("Antioxidant scan finished. Use /antioxidant status for the summary."));
        sender.sendMessage(started ? "Antioxidant scan and generation started asynchronously." : "A conversion is already running.");
    }

    private void sendStatus(CommandSender sender, boolean debug) {
        sender.sendMessage("Antioxidant | " + (service.running() ? "scanning" : "idle"));
        sender.sendMessage("Platform: " + compatibility.platform() + " | Minecraft: " + plugin.getServer().getMinecraftVersion() + " | Java: " + Runtime.version().feature());
        sender.sendMessage("Geyser: " + compatibility.geyserVersion() + " | Floodgate: " + compatibility.floodgateStatus());
        sender.sendMessage("ItemsAdder: " + compatibility.itemsAdderVersion() + " | Nexo: " + compatibility.nexoVersion());
        ConversionService.ConversionResult result = service.latest();
        if (result == null) {
            sender.sendMessage("No completed conversion yet.");
            return;
        }
        sender.sendMessage("Items: " + result.customItems() + " | mappings: " + result.mappings() + " | textures: " + result.textures() + " | models: " + result.models());
        sender.sendMessage("Pack: " + result.packStatus() + " | UUID: " + result.packUuid());
        if (debug) {
            sender.sendMessage("Base items: " + result.baseItems() + " | mapping file: " + result.mappingsPath());
            sender.sendMessage("Pack file: " + result.packPath() + " | warnings: " + result.warnings().size());
            for (String warning : result.warnings().stream().limit(8).toList()) sender.sendMessage("WARN: " + warning);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        String prefix = args[0].toLowerCase(java.util.Locale.ROOT);
        return SUBCOMMANDS.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}