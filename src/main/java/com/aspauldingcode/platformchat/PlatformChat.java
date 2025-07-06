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

// EaglerX Backend RPC imports (backend API)
import net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI;
import net.lax1dude.eaglercraft.backend.server.api.IEaglerPlayer;

public class PlatformChat extends JavaPlugin implements Listener {
    private boolean usePlaceholderAPI;
    private boolean useEaglerXBackend;
    private IEaglerXServerAPI<?> eaglerAPI;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        usePlaceholderAPI = config.getBoolean("use-placeholderapi", false);
        useEaglerXBackend = config.getBoolean("use-eaglerx-backend", true);
        
        // Try to initialize EaglerX Backend RPC support
        if (useEaglerXBackend) {
            try {
                // Check if EaglerX Backend API is available
                Class.forName("net.lax1dude.eaglercraft.backend.server.api.IEaglerXServerAPI");
                // Get the EaglerX server API instance from the backend RPC
                eaglerAPI = getEaglerXBackendAPI();
                if (eaglerAPI != null) {
                    getLogger().info("EaglerX Backend API support enabled.");
                } else {
                    useEaglerXBackend = false;
                    getLogger().warning("EaglerX Backend API not available, disabling Eaglercraft support.");
                }
            } catch (ClassNotFoundException e) {
                useEaglerXBackend = false;
                getLogger().warning("EaglerX Backend API not found, disabling Eaglercraft support.");
            } catch (Exception e) {
                useEaglerXBackend = false;
                getLogger().warning("Failed to initialize EaglerX Backend API support: " + e.getMessage());
            }
        }
        
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PlatformChat enabled" + 
            (usePlaceholderAPI ? " with PlaceholderAPI support" : "") +
            (useEaglerXBackend ? " and EaglerX Backend support." : "."));
    }
    
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        FileConfiguration config = getConfig();
        
        // Determine player platform
        PlayerPlatform platform = determinePlayerPlatform(player);
        
        // Get appropriate format based on platform
        String format = getFormatForPlatform(platform, config);
        
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
                       .replace("%message%", message);
        
        // Apply color codes
        format = ChatColor.translateAlternateColorCodes('&', format);
        
        // Cancel original event and broadcast custom format
        event.setCancelled(true);
        Bukkit.getServer().broadcastMessage(format);
    }
    
    private PlayerPlatform determinePlayerPlatform(Player player) {
        // Check for Bedrock first (Floodgate)
        if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
            return PlayerPlatform.BEDROCK;
        }
        
        // Check for Eaglercraft (EaglerX Backend API)
        if (useEaglerXBackend && eaglerAPI != null) {
            try {
                // Get the Eaglercraft player object
                IEaglerPlayer<?> eaglerPlayer = eaglerAPI.getPlayer(player.getUniqueId());
                if (eaglerPlayer != null && eaglerPlayer.isEaglerPlayer()) {
                    
                    // Check if it's a rewind player (1.5.2 via EaglerXRewind)
                    if (isRewindPlayer(eaglerPlayer)) {
                        return PlayerPlatform.EAGLERCRAFT_1_5_2;
                    } else {
                        // For non-rewind Eaglercraft players, get the protocol version
                        int protocolVersion = eaglerPlayer.getMinecraftProtocol();
                        return determineEaglercraftVersion(protocolVersion);
                    }
                }
            } catch (Exception e) {
                getLogger().warning("Error checking Eaglercraft player status: " + e.getMessage());
            }
        }
        
        // Default to Java Edition
        return PlayerPlatform.JAVA;
    }
    
    private boolean isRewindPlayer(IEaglerPlayer<?> eaglerPlayer) {
        try {
            // Check if the player has a rewind protocol attachment
            // This checks if the player is using EaglerXRewind (1.5.2)
            Object rewindAttachment = eaglerPlayer.getRewindAttachment();
            return rewindAttachment != null;
        } catch (Exception e) {
            // If method doesn't exist or fails, assume not a rewind player
            return false;
        }
    }
    
    private PlayerPlatform determineEaglercraftVersion(int protocolVersion) {
        // Map protocol versions to Eaglercraft versions
        // Based on your description: regular EaglerX can support 1.8 and 1.12.2
        switch (protocolVersion) {
            case 47:
                return PlayerPlatform.EAGLERCRAFT_1_8;
            case 335:
            case 338:
            case 340:
                return PlayerPlatform.EAGLERCRAFT_1_12_2;
            default:
                // Log unknown protocol version for debugging
                getLogger().info("Unknown Eaglercraft protocol version: " + protocolVersion);
                return PlayerPlatform.EAGLERCRAFT_UNKNOWN;
        }
    }
    
    private String getFormatForPlatform(PlayerPlatform platform, FileConfiguration config) {
        switch (platform) {
            case BEDROCK:
                return config.getString("bedrock-format", "&b[Bedrock] &a%player_name%&f: %message%");
            case EAGLERCRAFT_1_5_2:
                return config.getString("eaglercraft-1.5.2-format", "&e[Eaglercraft 1.5.2] &a%player_name%&f: %message%");
            case EAGLERCRAFT_1_8:
                return config.getString("eaglercraft-1.8-format", "&e[Eaglercraft 1.8] &a%player_name%&f: %message%");
            case EAGLERCRAFT_1_12_2:
                return config.getString("eaglercraft-1.12.2-format", "&e[Eaglercraft 1.12.2] &a%player_name%&f: %message%");
            case EAGLERCRAFT_UNKNOWN:
                return config.getString("eaglercraft-format", "&e[Eaglercraft] &a%player_name%&f: %message%");
            case JAVA:
            default:
                return config.getString("java-format", "&7[Java] &a%player_name%&f: %message%");
        }
    }
    
    // Get the EaglerX Backend API instance
    // This depends on how the backend RPC is set up
    private IEaglerXServerAPI<?> getEaglerXBackendAPI() {
        try {
            // The backend API should be available as a service or singleton
            // This might be something like:
            // return EaglerXBackendRPC.getServerAPI();
            // or
            // return EaglerXBackend.getInstance().getServerAPI();
            
            // For now, return null - you'll need to implement this based on
            // how your EaglerX Backend RPC is configured
            
            // Example implementation (adjust based on your setup):
            Class<?> backendClass = Class.forName("net.lax1dude.eaglercraft.backend.EaglerXBackend");
            Object backendInstance = backendClass.getMethod("getInstance").invoke(null);
            return (IEaglerXServerAPI<?>) backendClass.getMethod("getServerAPI").invoke(backendInstance);
            
        } catch (Exception e) {
            getLogger().warning("Failed to get EaglerX Backend API: " + e.getMessage());
            return null;
        }
    }
    
    private enum PlayerPlatform {
        JAVA,
        BEDROCK,
        EAGLERCRAFT_1_5_2,    // EaglerXRewind only
        EAGLERCRAFT_1_8,      // Regular EaglerX
        EAGLERCRAFT_1_12_2,   // Regular EaglerX
        EAGLERCRAFT_UNKNOWN   // Unknown Eaglercraft version
    }
}
