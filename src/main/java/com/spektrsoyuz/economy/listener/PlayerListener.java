package com.spektrsoyuz.economy.listener;

import com.spektrsoyuz.economy.EconomyPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

// Listener class for handling player events
public final class PlayerListener implements Listener {

    private final EconomyPlugin plugin;

    // Constructor
    public PlayerListener(final EconomyPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    // Handles player join
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        this.plugin.getAccountController().getAccount(player).ifPresentOrElse(account -> {
            // Found an existing account for the player
            final String username = player.getName();

            if (!(username.equalsIgnoreCase(account.getName()))) {
                // Set account name to player name
                account.setName(username);
            }

            this.plugin.getAccountController().addPlayerAccount(account);
        }, () -> {
            // Create new account for player
            this.plugin.getAccountController().createAccount(player.getUniqueId(), player.getName());
            this.plugin.getComponentLogger().info("Creating player account for '{}:{}'",
                    player.getUniqueId(),
                    player.getName()
            );
        });
    }

    // Handles player quit
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        this.plugin.getAccountController().getAccount(player).ifPresentOrElse(account -> {
            // Found an existing account for the player
            this.plugin.getAccountController().removePlayerAccount(account);
        }, () -> {
            // No account found
            this.plugin.getComponentLogger().error("No account found for player '{}:{}'",
                    player.getUniqueId(),
                    player.getName()
            );
        });
    }
}
