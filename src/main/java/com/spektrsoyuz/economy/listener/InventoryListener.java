package com.spektrsoyuz.economy.listener;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.CurrencyType;
import com.spektrsoyuz.economy.model.account.Transactor;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

/**
 * Listener class for inventory events.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class InventoryListener implements Listener {

    private final EconomyPlugin plugin;

    // Registers the listener
    public void register() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
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

                this.plugin.getEconomyController().depositItem(player, account, itemStack, valuePerItem);
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

            final ItemStack itemStack = itemToDeposit.clone();
            itemStack.setAmount(amountToDeposit);

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

            this.plugin.getEconomyController().depositItem(player, account, itemStack, valuePerItem);
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

            final ItemStack itemStack = cursorStack.clone();
            itemStack.setAmount(totalDraggedAmount);

            final int remainingAmount = cursorStack.getAmount() - totalDraggedAmount;
            final ItemStack remainingCursor = cursorStack.clone();
            remainingCursor.setAmount(remainingAmount);

            // Update the cursor on the next tick to prevent synchronization issues
            this.plugin.getServer().getScheduler().runTask(this.plugin,
                    () -> player.setItemOnCursor(remainingAmount <= 0 ? null : remainingCursor)
            );

            this.plugin.getEconomyController().depositItem(player, account, itemStack, valuePerItem);
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
                this.plugin.getEconomyController().handleTransactionFailure(player);
                return;
            }

            // Remove item
            item.setAmount(0);

            // Send message to player
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    String.format("command-deposit-%s", config.getType().name().toLowerCase()),
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

}
