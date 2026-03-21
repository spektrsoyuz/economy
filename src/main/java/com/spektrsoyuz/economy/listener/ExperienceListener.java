package com.spektrsoyuz.economy.listener;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.CurrencyType;
import com.spektrsoyuz.economy.model.account.Transactor;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import com.spektrsoyuz.economy.model.config.OptionsConfig;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerLevelChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.math.BigDecimal;

/**
 * Listener class for experience events.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class ExperienceListener implements Listener {

    private final EconomyPlugin plugin;

    // Registers the listener
    public void register() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler
    public void onExpBottleBreak(final ExpBottleEvent event) {
        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (!(config.getType() == CurrencyType.EXP)) return;

        // Set experience drop to fixed value
        event.setExperience(config.getExp().getCost());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExpChange(final PlayerExpChangeEvent event) {
        // Check if event has already been processed
        if (event.getAmount() == 0) {
            return;
        }

        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (!(config.getType() == CurrencyType.EXP)) return;

        final Player player = event.getPlayer();
        final int amount = event.getAmount();

        // Disable vanilla XP gain
        event.setAmount(0);

        if (amount <= 0) return;

        this.plugin.getAccountController().getPlayerAccount(player).ifPresent(account -> {
            // Convert raw XP points to a fraction of a level
            final double levelGain = (double) amount / config.getExp().getCost();
            final boolean success = account.addBalance(BigDecimal.valueOf(levelGain), Transactor.SERVER);

            if (!success) {
                // Transaction failed
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-transaction-failed",
                        this.plugin.getMiniMessage()
                ));
                EconomyUtils.playErrorSound(player);
            }
        });
    }

    @EventHandler
    public void onLevelChange(final PlayerLevelChangeEvent event) {
        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (!(config.getType() == CurrencyType.EXP)) return;

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

                final boolean success = account.subtractBalance(BigDecimal.valueOf(levelsSpent), Transactor.SERVER);

                if (!success) {
                    // Transaction failed
                    player.sendMessage(this.plugin.getConfigController().getMessage(
                            "error-transaction-failed",
                            this.plugin.getMiniMessage()
                    ));

                    player.setLevel(oldLevel);
                    EconomyUtils.playErrorSound(player);
                }
            }
        });
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (!(config.getType() == CurrencyType.EXP)) return;

        final Player player = event.getPlayer();
        event.setDroppedExp(0);
        event.setKeepLevel(true);

        this.plugin.getAccountController().getPlayerAccount(player).ifPresent(account -> {
            final OptionsConfig optionsConfig = this.plugin.getConfigController().getOptionsConfig();

            // Subtract from player balance on death
            if (optionsConfig.isLoseBalanceOnDeath()) {
                final BigDecimal amount = optionsConfig.getLoseBalanceOnDeathAmount();
                final String currency = EconomyUtils.format(this.plugin, amount);

                if (account.getBalance().compareTo(amount) < 0) return;
                final boolean accountSuccess = account.subtractBalance(amount, Transactor.SERVER);

                if (!accountSuccess) {
                    // Transaction failed
                    player.sendMessage(this.plugin.getConfigController().getMessage(
                            "error-transaction-failed",
                            this.plugin.getMiniMessage()
                    ));

                    EconomyUtils.playErrorSound(player);
                    return;
                }

                final Player killer = event.getEntity().getKiller();
                if (killer != null) {
                    // Add to killer balance
                    this.plugin.getAccountController().getPlayerAccount(killer).ifPresent(killerAccount -> {
                        final boolean killerSuccess = killerAccount.addBalance(amount, Transactor.SERVER);

                        if (!killerSuccess) {
                            // Transaction failed
                            player.sendMessage(this.plugin.getConfigController().getMessage(
                                    "error-transaction-failed",
                                    this.plugin.getMiniMessage()
                            ));
                            EconomyUtils.playErrorSound(player);
                            return;
                        }

                        killer.sendMessage(this.plugin.getConfigController().getMessage(
                                "economy-death-killer",
                                this.plugin.getMiniMessage(),
                                Placeholder.parsed("player", player.getName()),
                                Placeholder.parsed("currency", currency)
                        ));

                        killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
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
        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (!(config.getType() == CurrencyType.EXP)) return;

        // Sync XP bar
        this.plugin.getAccountController().updateExp(event.getPlayer());
    }

}
