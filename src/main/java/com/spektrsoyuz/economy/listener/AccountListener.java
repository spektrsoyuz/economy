package com.spektrsoyuz.economy.listener;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.CurrencyType;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

/**
 * Listener class for economy accounts.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class AccountListener implements Listener {

    private final EconomyPlugin plugin;

    // Registers the listener
    public void register() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

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

            final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();

            if (config.getType() == CurrencyType.EXP) {
                this.plugin.getAccountController().updateExp(player);
            }
        }, () -> {
            // Create new account for player
            this.plugin.getAccountController().createAccount(player.getUniqueId(), player.getName(), true);
            this.plugin.getComponentLogger().info("Creating player account for '{}:{}'",
                    player.getUniqueId(),
                    player.getName()
            );
        });
    }

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
