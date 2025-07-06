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

// EaglerXBackendRPC API imports
import net.lax1dude.eaglercraft.backend.rpc.api.bukkit.EaglerXBackendRPC;
import net.lax1dude.eaglercraft.backend.rpc.api.IEaglerXBackendRPC;

public class PlatformChat extends JavaPlugin implements Listener {
    
    private boolean usePlaceholderAPI;
    private boolean useEaglerXBackendRPC;
    private IEaglerXBackendRPC<Player> eaglerAPI;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        
        usePlaceholderAPI = config.getBoolean("use-placeholderapi", false);
        useEaglerXBackendRPC = config.getBoolean("use-eaglerxbackendrpc", true);
        
        // Initialize EaglerXBackendRPC API if available
        if (useEaglerXBackendRPC) {
            try {
                eaglerAPI = EaglerXBackendRPC.instance();
                getLogger().info("EaglerXBackendRPC API initialized successfully");
            } catch (Exception e) {
                getLogger().warning("Failed to initialize EaglerXBackendRPC API: " + e.getMessage());
                useEaglerXBackendRPC = false;
            }
        }
        
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PlatformChat enabled" + 
            (usePlaceholderAPI ? " with PlaceholderAPI support" : "") +
            (useEaglerXBackendRPC ? " with EaglerXBackendRPC support" : "") + ".");
    }
    
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        FileConfiguration config = getConfig();
        
        // Determine player platform
        String platformType = determinePlatform(player);
        String format = getFormatForPlatform(platformType, config);
        
        // Apply prefix if configured
        String prefix = config.getString("prefix", "");
        if (prefix != null && !prefix.isEmpty()) {
            format = prefix + format;
        }
        
        // Apply PlaceholderAPI if enabled
        if (usePlaceholderAPI && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            format = PlaceholderAPI.setPlaceholders(player, format);
        }
        
        // Replace placeholders
        format = format.replace("%player_name%", player.getName())
                      .replace("%message%", message)
                      .replace("%platform%", platformType);
        
        // Apply color codes
        format = ChatColor.translateAlternateColorCodes('&', format);
        
        // Cancel original event and broadcast custom format
        event.setCancelled(true);
        Bukkit.getServer().broadcastMessage(format);
    }
    
    private String determinePlatform(Player player) {
        // Check if player is from Bedrock (Floodgate)
        if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            return "bedrock";
        }
        
        // Check if player is from Eaglercraft (EaglerXBackendRPC)
        if (useEaglerXBackendRPC && eaglerAPI != null) {
            try {
                if (eaglerAPI.isEaglerPlayer(player)) {
                    // This is an Eaglercraft player
                    return getEaglerPlatformInfo(player);
                }
            } catch (Exception e) {
                getLogger().warning("Error checking Eaglercraft player status: " + e.getMessage());
            }
        }
        
        // Check protocol version for regular Java players
        // You might need to implement getMinecraftProtocol() based on your server setup
        int protocol = getMinecraftProtocol(player);
        return getJavaVersionFromProtocol(protocol);
    }
    
    private String getEaglerPlatformInfo(Player player) {
        try {
            // Check if this is a rewind player (1.5.2)
            if (eaglerAPI.isRewindPlayer(player)) {
                return "eaglercraft-1.5.2";
            }
            
            // For non-rewind players, try to get version info
            int protocol = eaglerAPI.getEaglerProtocol(player);
            switch (protocol) {
                case 47:
                    return "eaglercraft-1.8";
                case 335:
                    return "eaglercraft-1.12.2";
                default:
                    return "eaglercraft";
            }
        } catch (Exception e) {
            getLogger().warning("Error determining Eaglercraft version: " + e.getMessage());
            return "eaglercraft";
        }
    }
    
    private String getFormatForPlatform(String platform, FileConfiguration config) {
        switch (platform.toLowerCase()) {
            case "bedrock":
                return config.getString("bedrock-format", "&b[Bedrock] &a%player_name%&f: %message%");
            case "eaglercraft":
                return config.getString("eaglercraft-format", "&6[Eaglercraft] &a%player_name%&f: %message%");
            case "eaglercraft-1.5.2":
                return config.getString("eaglercraft-1.5.2-format", "&6[Eaglercraft 1.5.2] &a%player_name%&f: %message%");
            case "eaglercraft-1.8":
                return config.getString("eaglercraft-1.8-format", "&6[Eaglercraft 1.8] &a%player_name%&f: %message%");
            case "eaglercraft-1.12.2":
                return config.getString("eaglercraft-1.12.2-format", "&6[Eaglercraft 1.12.2] &a%player_name%&f: %message%");
            case "java-1.8":
                return config.getString("java-1.8-format", "&7[Java 1.8] &a%player_name%&f: %message%");
            case "java-1.12.2":
                return config.getString("java-1.12.2-format", "&7[Java 1.12.2] &a%player_name%&f: %message%");
            default:
                return config.getString("java-format", "&7[Java] &a%player_name%&f: %message%");
        }
    }
    
    private String getJavaVersionFromProtocol(int protocol) {
        switch (protocol) {
            case 47:
                return "java-1.8";
            case 335:
                return "java-1.12.2";
            // Add more protocol versions as needed
            default:
                return "java";
        }
    }
    
    private int getMinecraftProtocol(Player player) {
        // For regular Java players, you can try to get the protocol version
        // This implementation depends on your server version and available methods
        try {
            // For Paper/Spigot servers, you might be able to access this:
            // return player.getProtocolVersion(); // if available
            
            // Alternative approach using reflection (server-version dependent)
            // This is a simplified example - you'd need to implement based on your server
            return 47; // Default to 1.8 protocol for now
        } catch (Exception e) {
            getLogger().warning("Could not determine protocol version for player: " + player.getName());
            return 47; // Default fallback
        }
    }
    
    @Override
    public void onDisable() {
        getLogger().info("PlatformChat disabled.");
    }
}
