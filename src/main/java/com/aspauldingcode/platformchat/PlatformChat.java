package com.aspaulding.platformchat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import me.clip.placeholderapi.PlaceholderAPI;
import org.geysermc.floodgate.api.FloodgateApi;

public class PlatformChat extends JavaPlugin implements Listener {

    private boolean usePlaceholderAPI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        usePlaceholderAPI = config.getBoolean("use-placeholderapi", false);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PlatformChat enabled" + (usePlaceholderAPI ? " with PlaceholderAPI support." : "."));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        FileConfiguration config = getConfig();

        boolean isBedrock = FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());

        String format = isBedrock
                ? config.getString("bedrock-format", "&b[Bedrock] &a%player_name%&f: %message%")
                : config.getString("java-format", "&7[Java] &a%player_name%&f: %message%");

        String prefix = config.getString("prefix", "");
        if (prefix != null && !prefix.isEmpty()) {
            format = prefix + format;
        }

        if (usePlaceholderAPI && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        }

        format = format.replace("%player_name%", player.getName())
                       .replace("%message%", message);

        format = ChatColor.translateAlternateColorCodes('&', format);

        event.setCancelled(true);
        Bukkit.getServer().broadcastMessage(format);
    }
}

