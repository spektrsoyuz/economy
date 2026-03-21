package com.spektrsoyuz.economy.controller;

import com.mojang.brigadier.Command;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.account.Account;
import com.spektrsoyuz.economy.model.account.Transactor;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.*;

/**
 * Controller class for economy transactions.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class EconomyController {

    private final EconomyPlugin plugin;

    private final Map<UUID, BigDecimal> sessionTotals = new HashMap<>();
    private final Map<UUID, Integer> resetTasks = new HashMap<>();

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
            final int balance = currentBalance.intValue();

            // Cap the request by the account balance
            int amountToTry = Math.min(requestedAmount, balance);

            if (amountToTry <= 0) {
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-not-enough-balance",
                        this.plugin.getMiniMessage()
                ));

                EconomyUtils.playErrorSound(player);
                return;
            }

            // Calculate the item stacks
            List<ItemStack> stacksToGive = this.getFittingWithdrawalStacks(player, config, amountToTry);
            int finalAmount = amountToTry;

            // If the requested amount doesn't fit, we try to find the max that does
            if (stacksToGive == null) {
                finalAmount = this.calculateMaxFit(player, config, amountToTry);
                if (finalAmount <= 0) {
                    // Not enough inventory space
                    player.sendMessage(this.plugin.getConfigController().getMessage(
                            "error-not-enough-inventory-space",
                            this.plugin.getMiniMessage()
                    ));

                    EconomyUtils.playErrorSound(player);
                    return;
                }
                stacksToGive = this.buildStacks(config, finalAmount, true);
            }

            if (stacksToGive == null || stacksToGive.isEmpty()) return;

            // Subtract the amount from the player's account
            final BigDecimal amountToSubtract = BigDecimal.valueOf(finalAmount);
            final boolean success = account.subtractBalance(amountToSubtract, Transactor.SERVER);

            // Check if the transaction failed
            if (!success) {
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-transaction-failed",
                        this.plugin.getMiniMessage()
                ));
                EconomyUtils.playErrorSound(player);
                return;
            }

            // Distribute items to player
            for (final ItemStack stack : stacksToGive) {
                player.getInventory().addItem(stack);
            }

            // Disable auto-deposit if applicable
            if (account.isAutoDeposit()) {
                account.setAutoDeposit(false);

                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "command-deposit-auto-disable",
                        this.plugin.getMiniMessage()
                ));
            }

            // Send message to player
            final String currencyFormatted = EconomyUtils.format(this.plugin, amountToSubtract);
            final String messageKey = String.format("economy-%s-withdraw", config.getType().name().toLowerCase());

            player.sendMessage(this.plugin.getConfigController().getMessage(
                    messageKey,
                    this.plugin.getMiniMessage(),
                    Placeholder.parsed("amount", String.valueOf(finalAmount)),
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

    // Finds a combination of items that fits in the player's inventory
    private List<ItemStack> getFittingWithdrawalStacks(final Player player, final CurrencyConfig config, final int amount) {
        // Try the optimal approach first
        final List<ItemStack> optimalStacks = this.buildStacks(config, amount, true);
        if (optimalStacks != null && this.doesFit(player, optimalStacks)) {
            return optimalStacks;
        }

        // Fallback with the lowest value item
        final List<ItemStack> fallbackStacks = this.buildStacks(config, amount, false);
        if (fallbackStacks != null && this.doesFit(player, fallbackStacks)) {
            return fallbackStacks;
        }

        return null;
    }

    // Builds a list of ItemStacks for a specific amount
    private List<ItemStack> buildStacks(final CurrencyConfig config, final int amount, final boolean useOptimal) {
        final List<ItemStack> items = new ArrayList<>();
        int remainingToGive = amount;

        // Sort items based on their value per stack
        final List<Map.Entry<String, Integer>> sortedItems = this.getSortedStacks(config);

        if (useOptimal) {
            // Check highest value
            for (final Map.Entry<String, Integer> entry : sortedItems) {
                if (remainingToGive <= 0) break;

                final Material material = Material.matchMaterial(entry.getKey());
                if (material == null) continue;

                final int itemValue = entry.getValue();
                int countToGive = remainingToGive / itemValue;

                if (countToGive > 0) {
                    this.addStacksToList(items, material, countToGive);
                    remainingToGive %= itemValue;
                }
            }
        } else {
            // Check lowest value
            final Map.Entry<String, Integer> lowestValue = sortedItems.getLast();
            final Material material = Material.matchMaterial(lowestValue.getKey());
            final int itemValue = lowestValue.getValue();

            if (material != null && itemValue > 0) {
                // Only proceed if it divides perfectly
                if (remainingToGive % itemValue == 0) {
                    final int countToGive = remainingToGive / itemValue;
                    this.addStacksToList(items, material, countToGive);
                } else {
                    // Does not fit in inventory
                    return null;
                }
            } else {
                return null;
            }
        }

        return items;
    }

    // Gets a list of stacks sorted by value
    private List<Map.Entry<String, Integer>> getSortedStacks(final CurrencyConfig config) {
        final List<Map.Entry<String, Integer>> sortedItems = new ArrayList<>(config.getItems().entrySet());

        sortedItems.sort((a, b) -> {
            final Material matA = Material.matchMaterial(a.getKey());
            final Material matB = Material.matchMaterial(b.getKey());
            final int stackA = (matA != null) ? matA.getMaxStackSize() : 64;
            final int stackB = (matB != null) ? matB.getMaxStackSize() : 64;

            // Compare (Value * MaxStackSize)
            final long totalValA = (long) a.getValue() * stackA;
            final long totalValB = (long) b.getValue() * stackB;

            // Return highest density first
            return Long.compare(totalValB, totalValA);
        });
        return sortedItems;
    }

    // Breaks large amounts of a material into max-sized stacks
    private void addStacksToList(final List<ItemStack> list, final Material material, final int totalAmount) {
        int amountLeft = totalAmount;
        while (amountLeft > 0) {
            final int stackSize = Math.min(amountLeft, material.getMaxStackSize());
            list.add(new ItemStack(material, stackSize));
            amountLeft -= stackSize;
        }
    }

    // Simulates adding items to a virtual copy of the player's inventory
    private boolean doesFit(final Player player, final List<ItemStack> itemsToGive) {
        final Inventory inventory = this.plugin.getServer().createInventory(null, 36);
        inventory.setContents(player.getInventory().getStorageContents());

        final Map<Integer, ItemStack> leftovers = inventory.addItem(itemsToGive.toArray(new ItemStack[0]));
        return leftovers.isEmpty();
    }

    // Calculates the maximum amount of currency that can fit in a player's inventory
    private int calculateMaxFit(final Player player, final CurrencyConfig config, int maxAmount) {
        final Inventory inventory = this.plugin.getServer().createInventory(null, 36);
        inventory.setContents(player.getInventory().getStorageContents());

        int totalFittedValue = 0;
        int remainingToFit = maxAmount;

        // Iterate through items starting with the highest density (value * maxStackSize)
        for (final Map.Entry<String, Integer> entry : this.getSortedStacks(config)) {
            if (remainingToFit <= 0) break;

            final Material material = Material.matchMaterial(entry.getKey());
            if (material == null) continue;

            final int itemValue = entry.getValue();
            final int neededItems = remainingToFit / itemValue;

            if (neededItems > 0) {
                // Break the needed items into max stack sizes
                int itemsLeft = neededItems;
                final List<ItemStack> stacksToAdd = new ArrayList<>();

                while (itemsLeft > 0) {
                    int stackSize = Math.min(itemsLeft, material.getMaxStackSize());
                    stacksToAdd.add(new ItemStack(material, stackSize));
                    itemsLeft -= stackSize;
                }

                // Attempt to add all needed items to the dummy inventory
                final Map<Integer, ItemStack> leftovers = inventory.addItem(stacksToAdd.toArray(new ItemStack[0]));

                // Calculate how many items actually fit
                int itemsNotAdded = 0;
                for (final ItemStack leftover : leftovers.values()) {
                    itemsNotAdded += leftover.getAmount();
                }

                final int itemsAdded = neededItems - itemsNotAdded;
                final int valueAdded = itemsAdded * itemValue;

                totalFittedValue += valueAdded;
                remainingToFit -= valueAdded;
            }
        }

        return totalFittedValue;
    }

    // Model record for the value of an inventory slot
    private record SlotValue(int slot, int valuePerItem) {
    }

    // Handles a standard transaction failure
    public void handleTransactionFailure(final Player player) {
        player.sendMessage(this.plugin.getConfigController().getMessage(
                "error-transaction-failed",
                this.plugin.getMiniMessage()
        ));
        EconomyUtils.playErrorSound(player);
    }

    // Deposits an item's value into a player's account and updates their action bar
    public void depositItem(
            final Player player,
            final Account account,
            final ItemStack item,
            final int valuePerItem
    ) {
        final int count = item.getAmount();
        final BigDecimal totalValue = BigDecimal.valueOf((long) count * valuePerItem);

        final boolean success = account.addBalance(totalValue, Transactor.SERVER);

        if (!success) {
            this.handleTransactionFailure(player);
            return;
        }

        final UUID uuid = player.getUniqueId();

        // Update the running total for this session
        BigDecimal currentSessionTotal = this.sessionTotals.getOrDefault(uuid, BigDecimal.ZERO);
        currentSessionTotal = currentSessionTotal.add(totalValue);
        this.sessionTotals.put(uuid, currentSessionTotal);

        // Cancel any existing reset task
        if (this.resetTasks.containsKey(uuid)) {
            this.plugin.getServer().getScheduler().cancelTask(this.resetTasks.get(uuid));
        }

        // Send the action bar to the player
        player.sendActionBar(this.plugin.getConfigController().getMessage(
                "command-deposit-auto",
                this.plugin.getMiniMessage(),
                Placeholder.parsed("currency", EconomyUtils.format(this.plugin, currentSessionTotal))
        ));

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.5f);

        // Schedule the session reset
        final int taskId = this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            this.sessionTotals.remove(uuid);
            this.resetTasks.remove(uuid);
        }, 30L).getTaskId();

        this.resetTasks.put(uuid, taskId);
    }

}
