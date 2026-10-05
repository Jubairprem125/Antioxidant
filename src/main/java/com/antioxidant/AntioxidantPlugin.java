package com.antioxidant;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class AntioxidantPlugin extends JavaPlugin implements Listener {
    private CompatibilityManager compatibility;
    private ConversionService conversionService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        compatibility = new CompatibilityManager(this);
        conversionService = new ConversionService(this, compatibility);
        Bukkit.getPluginManager().registerEvents(this, this);
        AntioxidantCommand command = new AntioxidantCommand(this, conversionService, compatibility);
        getCommand("antioxidant").setExecutor(command);
        getCommand("antioxidant").setTabCompleter(command);
        if (getConfig().getBoolean("enable-auto-scan", true) && getConfig().getBoolean("enable-auto-generate", true)) {
            Bukkit.getScheduler().runTaskLater(this, this::startInitialConversionWhenReady, 40L);
        }
        getLogger().info("Enabled on " + Bukkit.getName() + " (Minecraft " + Bukkit.getMinecraftVersion() + ", Java " + Runtime.version().feature() + "). Geyser: " + compatibility.geyserVersion() + ".");
    }

    private void startInitialConversionWhenReady() {
        if (!compatibility.itemsAdderLoaded()) {
            Bukkit.getScheduler().runTaskLater(this, this::startInitialConversionWhenReady, 20L);
            return;
        }
        if (!conversionService.start(getConfig().getBoolean("itemsadder.enabled", true),
                getConfig().getBoolean("nexo.enabled", true), null)) {
            getLogger().warning("Initial conversion was not started because another scan is active.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!compatibility.isBedrockPlayer(event.getPlayer().getUniqueId())) return;
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("Bedrock player joined: " + event.getPlayer().getName() + "; generated pack status: " +
                    (conversionService.latest() == null ? "not generated yet" : conversionService.latest().packStatus()));
        }
    }

    @Override
    public void onDisable() {
        if (conversionService != null) conversionService.shutdown();
    }

    public CompatibilityManager compatibility() { return compatibility; }
    public ConversionService conversionService() { return conversionService; }
}