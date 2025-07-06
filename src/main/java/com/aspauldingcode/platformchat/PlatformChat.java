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

// EaglerXBackendRPC imports
import net.lax1dude.eaglercraft.backend.rpc.api.bukkit.EaglerXBackendRPC;
import net.lax1dude.eaglercraft.backend.rpc.api.IEaglerXBackendRPC;
import net.lax1dude.eaglercraft.backend.rpc.api.IPlayerRPC;
import net.lax1dude.eaglercraft.backend.rpc.api.IEaglerPlayerRPC;

public class PlatformChat extends JavaPlugin implements Listener {
    
    private boolean usePlaceholderAPI;
    private boolean useEaglerXBackendRPC;
    private IEaglerXBackendRPC<Player> eaglerAPI;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        usePlaceholderAPI = config.getBoolean("use-placeholderapi", false);
        
        // Check if EaglerXBackendRPC is available
        useEaglerXBackendRPC = checkEaglerXBackendRPC();
        
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PlatformChat enabled" + 
            (usePlaceholderAPI ? " with PlaceholderAPI support" : "") +
            (useEaglerXBackendRPC ? " with EaglerXBackendRPC support" : "") + ".");
    }
    
    private boolean checkEaglerXBackendRPC() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("EaglercraftXBackendRPC")) {
                eaglerAPI = EaglerXBackendRPC.instance();
                return true;
            }
        } catch (Exception e) {
            getLogger().warning("EaglerXBackendRPC not available: " + e.getMessage());
        }
        return false;
    }
    
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        FileConfiguration config = getConfig();
        
        // Determine platform
        String platform = determinePlatform(player);
        String format = getFormatForPlatform(platform, config);
        
        // Add prefix if configured
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
                      .replace("%platform%", platform);
        
        // Apply color codes
        format = ChatColor.translateAlternateColorCodes('&', format);
        
        // Cancel original event and broadcast formatted message
        event.setCancelled(true);
        Bukkit.getServer().broadcastMessage(format);
    }
    
    private String determinePlatform(Player player) {
        // Check for Bedrock first (via Floodgate)
        try {
            if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                return "Bedrock";
            }
        } catch (Exception e) {
            // Floodgate not available or error - continue checking
        }
        
        // Check for Eaglercraft if EaglerXBackendRPC is available
        if (useEaglerXBackendRPC && eaglerAPI != null) {
            try {
                IPlayerRPC playerRPC = eaglerAPI.getPlayer(player);
                if (playerRPC != null) {
                    // Check if this is an Eagler player
                    if (playerRPC.isEaglerPlayer()) {
                        IEaglerPlayerRPC eaglerPlayer = playerRPC.asEaglerPlayer();
                        
                        if (eaglerPlayer.isEaglerXRewindPlayer()) {
                            // This is Eaglercraft 1.5.2 (rewind)
                            int rewindVersion = eaglerPlayer.getRewindProtocolVersion();
                            return "Eaglercraft 1.5.2 (v" + rewindVersion + ")";
                        } else {
                            // This is regular Eaglercraft, check protocol version
                            int protocol = eaglerPlayer.getMinecraftProtocol();
                            return getEaglerVersionFromProtocol(protocol);
                        }
                    }
                }
            } catch (NoSuchMethodError e) {
                getLogger().warning("EaglerXBackendRPC API method not found: " + e.getMessage());
                getLogger().warning("You may need to update your EaglerXBackendRPC plugin version");
            } catch (Exception e) {
                getLogger().warning("Error checking Eaglercraft status: " + e.getMessage());
            }
        }
        
        // Default to Java
        return "Java";
    }
    
    private String getEaglerVersionFromProtocol(int protocol) {
        // Map protocol versions to Minecraft versions for Eaglercraft
        switch (protocol) {
            case 47:
                return "Eaglercraft 1.8";
            case 335:
                return "Eaglercraft 1.12.2";
            default:
                return "Eaglercraft (Unknown)";
        }
    }
    
    private String getFormatForPlatform(String platform, FileConfiguration config) {
        switch (platform) {
            case "Bedrock":
                return config.getString("bedrock-format", "&b[Bedrock] &a%player_name%&f: %message%");
            case "Eaglercraft 1.5.2":
                return config.getString("eaglercraft-1.5.2-format", "&e[Eaglercraft 1.5.2] &a%player_name%&f: %message%");
            case "Eaglercraft 1.8":
                return config.getString("eaglercraft-1.8-format", "&e[Eaglercraft 1.8] &a%player_name%&f: %message%");
            case "Eaglercraft 1.12.2":
                return config.getString("eaglercraft-1.12.2-format", "&e[Eaglercraft 1.12.2] &a%player_name%&f: %message%");
            case "Eaglercraft (Unknown)":
                return config.getString("eaglercraft-unknown-format", "&e[Eaglercraft] &a%player_name%&f: %message%");
            default:
                return config.getString("java-format", "&7[Java] &a%player_name%&f: %message%");
        }
    }
}
