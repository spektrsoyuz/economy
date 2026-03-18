package com.spektrsoyuz.economy.listener;

import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.account.Transactor;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import com.spektrsoyuz.economy.model.config.OptionsConfig;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.math.BigDecimal;

/**
 * Listener class for player events.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class PlayerListener implements Listener {

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

            final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();

            if (currencyConfig.getType().equals("exp")) {
                this.plugin.getAccountController().updateExp(player);
            }
        }, () -> {
            // Create new account for player
            this.plugin.getAccountController().createAccount(player.getUniqueId(), player.getName());
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

    @EventHandler
    public void onExpChange(final PlayerExpChangeEvent event) {
        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();
        if (!currencyConfig.getType().equals("exp")) return;

        final Player player = event.getPlayer();
        final int amount = event.getAmount();

        // Disable vanilla XP gain
        event.setAmount(0);

        if (amount <= 0) return;

        this.plugin.getAccountController().getPlayerAccount(player).ifPresent(account -> {
            // Convert raw XP points to a fraction of a level
            final double levelGain = (double) amount / Constants.LEVEL_COST;

            account.addBalance(BigDecimal.valueOf(levelGain), Transactor.SERVER);
        });
    }

    @EventHandler
    public void onLevelChange(final PlayerLevelChangeEvent event) {
        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();
        if (!currencyConfig.getType().equals("exp")) return;

        final Player player = event.getPlayer();
        final int oldLevel = event.getOldLevel();
        final int newLevel = event.getNewLevel();

        this.plugin.getAccountController().getPlayerAccount(player).ifPresent(account -> {
            final int expectedLevel = account.getBalance().intValue();
            if (newLevel == expectedLevel) {
                return;
            }

            // Check if the level changed
            if (newLevel < oldLevel && !player.isDead()) {
                final int levelsSpent = oldLevel - newLevel;

                account.subtractBalance(BigDecimal.valueOf(levelsSpent), Transactor.SERVER);
            }
        });
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();
        if (!currencyConfig.getType().equals("exp")) return;

        final Player player = event.getPlayer();
        event.setDroppedExp(0);
        event.setKeepLevel(true);

        this.plugin.getAccountController().getPlayerAccount(player).ifPresent(account -> {
            final OptionsConfig optionsConfig = this.plugin.getConfigController().getOptionsConfig();

            // Subtract from player balance on death
            if (optionsConfig.isLoseBalanceOnDeath()) {
                final BigDecimal amount = optionsConfig.getLoseBalanceOnDeathAmount();
                final String currency = EconomyUtils.format(this.plugin, amount);
                account.subtractBalance(amount, Transactor.SERVER);

                final Player killer = event.getEntity().getKiller();
                if (killer != null) {
                    // Add to killer balance
                    this.plugin.getAccountController().getPlayerAccount(killer).ifPresent(killerAccount -> {
                        killerAccount.addBalance(amount, Transactor.SERVER);

                        killer.sendMessage(this.plugin.getConfigController().getMessage(
                                "economy-death-killer",
                                this.plugin.getMiniMessage(),
                                Placeholder.parsed("player", player.getName()),
                                Placeholder.parsed("currency", currency)
                        ));
                    });

                    // Send killed message to player
                    player.sendMessage(this.plugin.getConfigController().getMessage(
                            "economy-death-killed",
                            this.plugin.getMiniMessage(),
                            Placeholder.parsed("killer", killer.getName()),
                            Placeholder.parsed("currency", currency)
                    ));
                } else {
                    // Send death message to player
                    player.sendMessage(this.plugin.getConfigController().getMessage(
                            "economy-death",
                            this.plugin.getMiniMessage(),
                            Placeholder.parsed("currency", currency)
                    ));
                }
            }
        });
    }

    @EventHandler
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();
        if (!currencyConfig.getType().equals("exp")) return;

        // Sync XP bar
        this.plugin.getAccountController().updateExp(event.getPlayer());
    }

}
