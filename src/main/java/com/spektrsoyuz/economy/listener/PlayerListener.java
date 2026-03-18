package com.spektrsoyuz.economy.listener;

import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.CurrencyType;
import com.spektrsoyuz.economy.model.account.Account;
import com.spektrsoyuz.economy.model.account.Transactor;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import com.spektrsoyuz.economy.model.config.OptionsConfig;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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

    @EventHandler
    public void onExpChange(final PlayerExpChangeEvent event) {
        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (!(config.getType() == CurrencyType.EXP)) return;

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

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (config.getType() != CurrencyType.ITEM && config.getType() != CurrencyType.EXP) return;

        final ItemStack item = event.getItem();

        if (item == null) return;
        if (!event.getAction().isRightClick()) return;
        if (!event.getPlayer().isSneaking()) return;

        final int valuePerItem = config.getItemValue(item.getType());
        if (valuePerItem > 0) {
            this.processItemExchange(event, item, config, valuePerItem);
        }
    }

    @EventHandler
    public void onPickupItem(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (config.getType() != CurrencyType.ITEM && config.getType() != CurrencyType.EXP) return;

        this.plugin.getAccountController().getPlayerAccount(player).ifPresentOrElse(account -> {
            // Account found
            if (!account.isAutoDeposit()) return;

            final ItemStack itemStack = event.getItem().getItemStack();
            final int valuePerItem = config.getItemValue(itemStack.getType());

            if (valuePerItem > 0) {
                // Cancel the item pickup and deposit the value instead
                event.setCancelled(true);
                event.getItem().remove();

                this.depositItem(player, account, itemStack, valuePerItem);
            }
        }, () -> {
            // No account found
            this.plugin.getComponentLogger().error(
                    "Auto deposit on item pickup failed, account not found for player '{}'",
                    player.getName()
            );
        });
    }

    @EventHandler
    public void onInventoryMove(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (config.getType() != CurrencyType.ITEM && config.getType() != CurrencyType.EXP) return;

        // Skip auto-deposit if the player is placing items into a container
        if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() != player) {
            if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                return;
            }
        }

        ItemStack itemToDeposit;
        int amountToDeposit;

        // Identify the action and the amount to be processed
        switch (event.getAction()) {
            case PLACE_ALL:
            case SWAP_WITH_CURSOR, PLACE_SOME:
                itemToDeposit = event.getCursor();
                amountToDeposit = itemToDeposit.getAmount();
                break;
            case PLACE_ONE:
                itemToDeposit = event.getCursor();
                amountToDeposit = 1;
                break;
            case MOVE_TO_OTHER_INVENTORY:
                itemToDeposit = event.getCurrentItem();
                amountToDeposit = (itemToDeposit != null) ? itemToDeposit.getAmount() : 0;
                break;
            default:
                return;
        }

        if (itemToDeposit == null || itemToDeposit.getType().isAir()) return;

        final int valuePerItem = config.getItemValue(itemToDeposit.getType());
        if (valuePerItem <= 0) return;

        this.plugin.getAccountController().getPlayerAccount(player).ifPresentOrElse(account -> {
            if (!account.isAutoDeposit()) return;

            // Prevent the item from actually moving into the slot
            event.setCancelled(true);

            final ItemStack depositStack = itemToDeposit.clone();
            depositStack.setAmount(amountToDeposit);

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                // Remove the item from the inventory slot for shift-clicks
                event.setCurrentItem(null);
            } else {
                // Reduce the amount on the cursor for placement actions
                int remaining = itemToDeposit.getAmount() - amountToDeposit;
                ItemStack cursorClone = itemToDeposit.clone();
                cursorClone.setAmount(remaining);
                player.setItemOnCursor(remaining <= 0 ? null : cursorClone);
            }

            this.depositItem(player, account, depositStack, valuePerItem);
        }, () -> {
            // No account found
            this.plugin.getComponentLogger().error(
                    "Auto deposit on item move failed, account not found for player '{}'",
                    player.getName()
            );
        });
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (config.getType() != CurrencyType.ITEM && config.getType() != CurrencyType.EXP) return;

        // Check if any part of the drag is entering an external inventory
        final int topInventorySize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topInventorySize) {
                return;
            }
        }

        final ItemStack cursorStack = event.getOldCursor();
        if (cursorStack.getType().isAir()) return;

        final int valuePerItem = config.getItemValue(cursorStack.getType());
        if (valuePerItem <= 0) return;

        this.plugin.getAccountController().getPlayerAccount(player).ifPresentOrElse(account -> {
            if (!account.isAutoDeposit()) return;

            // Calculate the sum of all items distributed
            int totalDraggedAmount = 0;
            for (final ItemStack item : event.getNewItems().values()) {
                totalDraggedAmount += item.getAmount();
            }

            if (totalDraggedAmount <= 0) return;

            event.setCancelled(true);

            final ItemStack depositStack = cursorStack.clone();
            depositStack.setAmount(totalDraggedAmount);

            final int remainingAmount = cursorStack.getAmount() - totalDraggedAmount;
            final ItemStack remainingCursor = cursorStack.clone();
            remainingCursor.setAmount(remainingAmount);

            // Update the cursor on the next tick to prevent synchronization issues
            this.plugin.getServer().getScheduler().runTask(this.plugin,
                    () -> player.setItemOnCursor(remainingAmount <= 0 ? null : remainingCursor)
            );

            this.depositItem(player, account, depositStack, valuePerItem);
        }, () -> {
            // No account found
            this.plugin.getComponentLogger().error(
                    "Auto deposit on item drag failed, account not found for player '{}'",
                    player.getName()
            );
        });
    }

    // Handles currency conversion
    private void processItemExchange(
            final PlayerInteractEvent event,
            final ItemStack item,
            final CurrencyConfig config,
            final int valuePerItem
    ) {
        final Player player = event.getPlayer();

        this.plugin.getAccountController().getPlayerAccount(player).ifPresentOrElse(account -> {
            // Account found
            event.setCancelled(true);

            // Handle transaction
            final int count = item.getAmount();
            final int totalValue = count * valuePerItem;
            final BigDecimal valueBD = BigDecimal.valueOf(totalValue);

            final boolean success = account.addBalance(valueBD, Transactor.SERVER);

            if (!success) {
                this.handleTransactionFailure(player);
                return;
            }

            // Remove item
            item.setAmount(0);

            // Send message to player
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    String.format("economy-%s-deposit", config.getType().name().toLowerCase()),
                    this.plugin.getMiniMessage(),
                    Placeholder.parsed("amount", String.valueOf(totalValue)),
                    Placeholder.parsed("currency", EconomyUtils.format(this.plugin, valueBD))
            ));

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }, () -> {
            // No account found
            this.plugin.getComponentLogger().error(
                    "Item exchange failed, account not found for player '{}'",
                    player.getName()
            );
        });
    }

    // Handles a transaction failure
    private void handleTransactionFailure(final Player player) {
        player.sendMessage(this.plugin.getConfigController().getMessage(
                "error-transaction-failed",
                this.plugin.getMiniMessage()
        ));

        EconomyUtils.playErrorSound(player);
    }

    // Deposits an item into a player's account
    private void depositItem(
            Player player,
            Account account,
            ItemStack item,
            int valuePerItem
    ) {
        final int count = item.getAmount();
        final BigDecimal totalValue = BigDecimal.valueOf((long) count * valuePerItem);

        final boolean success = account.addBalance(totalValue, Transactor.SERVER);

        if (!success) {
            // Transaction failed
            this.handleTransactionFailure(player);
            return;
        }

        // Send message to player
        player.sendActionBar(this.plugin.getConfigController().getMessage(
                "command-deposit-auto",
                this.plugin.getMiniMessage(),
                Placeholder.parsed("currency", EconomyUtils.format(this.plugin, totalValue))
        ));

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);
    }

}
