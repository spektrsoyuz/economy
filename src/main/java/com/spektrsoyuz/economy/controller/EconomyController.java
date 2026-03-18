package com.spektrsoyuz.economy.controller;

import com.mojang.brigadier.Command;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.account.Transactor;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller class for economy transactions.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class EconomyController {

    private final EconomyPlugin plugin;

    // Handles a deposit transaction
    public int handleDeposit(final Player player, final int amount, final CurrencyConfig config) {
        this.plugin.getAccountController().getAccount(player).ifPresentOrElse(account -> {
            // Calculate total value available in inventory
            long totalValueInInventory = 0;
            for (final ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType().isAir()) continue;
                int value = config.getItemValue(item.getType());

                if (value > 0) {
                    totalValueInInventory += (long) value * item.getAmount();
                }
            }

            // Check if player has enough value
            if (totalValueInInventory < amount) {
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-not-enough-balance",
                        this.plugin.getMiniMessage(),
                        Placeholder.parsed("currency", config.getNamePlural())
                ));

                EconomyUtils.playErrorSound(player);
                return;
            }

            // Sort currencies by value
            final List<SlotValue> currencySlots = new ArrayList<>();

            // Iterate over player inventory
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                final ItemStack item = player.getInventory().getItem(i);
                if (item == null) continue;
                int val = config.getItemValue(item.getType());

                if (val > 0) {
                    currencySlots.add(new SlotValue(i, val));
                }
            }

            // Sort higher valued items first
            currencySlots.sort((a, b) -> Integer.compare(b.valuePerItem(), a.valuePerItem()));

            // Remove value from inventory
            int remainingValueToRemove = amount;

            for (final SlotValue entry : currencySlots) {
                if (remainingValueToRemove <= 0) break;

                ItemStack item = player.getInventory().getItem(entry.slot());
                if (item == null) continue;

                int stackValue = entry.valuePerItem() * item.getAmount();

                if (stackValue <= remainingValueToRemove) {
                    // Remove the entire stack
                    remainingValueToRemove -= stackValue;
                    player.getInventory().setItem(entry.slot(), null);
                } else {
                    // Only remove a portion of the stack
                    final int itemsToTake = (int) Math.ceil((double) remainingValueToRemove / entry.valuePerItem());
                    final int totalValueTaken = itemsToTake * entry.valuePerItem();
                    final int changeDue = totalValueTaken - remainingValueToRemove;

                    item.setAmount(item.getAmount() - itemsToTake);
                    remainingValueToRemove = 0;

                    // Provide change if the item value was greater than the remaining debt
                    if (changeDue > 0) {
                        EconomyUtils.distributeItems(player, config, amount);
                    }
                }
            }

            // Update account balance
            final BigDecimal amountDecimal = BigDecimal.valueOf(amount);
            final boolean success = account.addBalance(amountDecimal, Transactor.SERVER);

            // Check if transaction failed
            if (!success) {
                // Refund items to play
                EconomyUtils.distributeItems(player, config, amount);

                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-transaction-failed",
                        this.plugin.getMiniMessage()
                ));

                EconomyUtils.playErrorSound(player);
                return;
            }

            // Send message to player
            final String currencyFormatted = EconomyUtils.format(this.plugin, amountDecimal);
            final String messageKey = String.format("economy-%s-deposit", config.getType().name().toLowerCase());

            player.sendMessage(this.plugin.getConfigController().getMessage(
                    messageKey,
                    this.plugin.getMiniMessage(),
                    Placeholder.parsed("amount", String.valueOf(amount)),
                    Placeholder.parsed("currency", currencyFormatted)
            ));

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }, () -> {
            // No account found
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-account-not-found",
                    this.plugin.getMiniMessage()
            ));
        });

        return Command.SINGLE_SUCCESS;
    }

    // Handles a withdrawal transaction
    public int handleWithdrawal(final Player player, final int requestedAmount, final CurrencyConfig config) {
        this.plugin.getAccountController().getAccount(player).ifPresentOrElse(account -> {
            final BigDecimal currentBalance = account.getBalance();

            // Determine how much value can be withdrawn
            final int availableSpace = this.calculateAvailableSpace(player, config);
            final int balanceInt = currentBalance.intValue();
            final int actualWithdrawAmount = Math.min(requestedAmount, Math.min(balanceInt, availableSpace));

            // Error handling for edge cases
            if (actualWithdrawAmount <= 0) {
                final String errorKey = (balanceInt <= 0)
                        ? "error-not-enough-balance"
                        : "error-not-enough-inventory-space";

                player.sendMessage(this.plugin.getConfigController().getMessage(
                        errorKey,
                        this.plugin.getMiniMessage()
                ));

                EconomyUtils.playErrorSound(player);
                return;
            }

            // Subtract the partial/full amount from player account
            final BigDecimal amountToSubtract = BigDecimal.valueOf(actualWithdrawAmount);
            final boolean success = account.subtractBalance(amountToSubtract, Transactor.SERVER);

            if (!success) {
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-transaction-failed",
                        this.plugin.getMiniMessage()
                ));
                EconomyUtils.playErrorSound(player);
                return;
            }

            // Distribute items
            EconomyUtils.distributeItems(player, config, actualWithdrawAmount);

            // Send message to player
            final String currencyFormatted = EconomyUtils.format(this.plugin, amountToSubtract);
            final String messageKey = String.format("economy-%s-withdraw", config.getType().name().toLowerCase());

            player.sendMessage(this.plugin.getConfigController().getMessage(
                    messageKey,
                    this.plugin.getMiniMessage(),
                    Placeholder.parsed("amount", String.valueOf(actualWithdrawAmount)),
                    Placeholder.parsed("currency", currencyFormatted)
            ));

            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }, () -> {
            // No account found
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-account-not-found",
                    this.plugin.getMiniMessage()
            ));
        });

        return Command.SINGLE_SUCCESS;
    }

    // Calculates how much total currency value the player can fit in their inventory
    private int calculateAvailableSpace(final Player player, final CurrencyConfig config) {
        int totalValueSpace = 0;
        final int maxValuation = config.getItems().values().stream().max(Integer::compare).orElse(1);

        for (final ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                // Assume empty slots can hold a full stack of the highest value item
                totalValueSpace += (maxValuation * 64);
            } else {
                int value = config.getItemValue(item.getType());
                if (value > 0) {
                    totalValueSpace += (64 - item.getAmount()) * value;
                }
            }
        }

        return totalValueSpace;
    }

    // Model record for the value of an inventory slot
    private record SlotValue(int slot, int valuePerItem) {
    }

}
