package com.spektrsoyuz.economy.listener;

import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.account.Transactor;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import com.spektrsoyuz.economy.model.config.OptionsConfig;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

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

            final boolean success = account.addBalance(BigDecimal.valueOf(levelGain), Transactor.SERVER);

            if (!success) {
                // Transaction failed
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-transaction-failed",
                        this.plugin.getMiniMessage()
                ));
                player.setExp(player.getExp() - amount);
            }
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

                final boolean success = account.subtractBalance(BigDecimal.valueOf(levelsSpent), Transactor.SERVER);

                if (!success) {
                    // Transaction failed
                    player.sendMessage(this.plugin.getConfigController().getMessage(
                            "error-transaction-failed",
                            this.plugin.getMiniMessage()
                    ));
                    player.setLevel(oldLevel);
                }
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

                final boolean accountSuccess = account.subtractBalance(amount, Transactor.SERVER);

                if (!accountSuccess) {
                    // Transaction failed
                    player.sendMessage(this.plugin.getConfigController().getMessage(
                            "error-transaction-failed",
                            this.plugin.getMiniMessage()
                    ));
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
                            return;
                        }

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

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();
        if (!currencyConfig.getType().equals("exp")) return;

        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();

        this.plugin.getAccountController().getPlayerAccount(player).ifPresent(account -> {
            // Check if the item is an experience bottle
            if (item != null && item.getType() == Material.EXPERIENCE_BOTTLE) {
                // Check if player is right-clicking
                if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    // Check if player is sneaking
                    if (player.isSneaking()) {
                        event.setCancelled(true);

                        // Add balance to player account
                        final int amount = item.getAmount();
                        final boolean success = account.addBalance(BigDecimal.valueOf(amount), Transactor.SERVER);

                        if (!success) {
                            // Transaction failed
                            player.sendMessage(this.plugin.getConfigController().getMessage(
                                    "error-transaction-failed",
                                    this.plugin.getMiniMessage()
                            ));

                            event.setCancelled(true);
                            return;
                        }

                        // Remove item from inventory
                        if (player.getInventory().getItemInMainHand().equals(item)) {
                            player.getInventory().setItemInMainHand(null);
                        } else if (player.getInventory().getItemInOffHand().equals(item)) {
                            player.getInventory().setItemInOffHand(null);
                        }

                        // Send message to player
                        final String currency = EconomyUtils.format(this.plugin, BigDecimal.valueOf(amount));

                        player.sendMessage(this.plugin.getConfigController().getMessage(
                                "economy-exp-discard",
                                this.plugin.getMiniMessage(),
                                Placeholder.parsed("amount", String.valueOf(amount)),
                                Placeholder.parsed("currency", currency)
                        ));
                    }
                }
            }
        });
    }

}
