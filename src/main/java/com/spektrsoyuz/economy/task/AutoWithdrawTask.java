package com.spektrsoyuz.economy.task;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.CurrencyType;
import com.spektrsoyuz.economy.model.account.Transactor;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Asynchronous task for handling automatic withdrawal.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class AutoWithdrawTask implements Runnable {

    private final EconomyPlugin plugin;

    @Override
    public void run() {
        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        if (config.getType() != CurrencyType.ITEM && config.getType() != CurrencyType.EXP) return;

        // Get items sorted by density (safe to do async as it just relies on Enums)
        final List<Map.Entry<String, Integer>> sortedItems = this.plugin.getEconomyController().getSortedStacks(config);

        for (final Player player : this.plugin.getServer().getOnlinePlayers()) {
            this.plugin.getAccountController().getPlayerAccount(player).ifPresentOrElse(account -> {
                // Account found
                if (!account.isAutoWithdraw()) return;

                // Offload Bukkit API interactions to the main thread
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                    // Check player balance synchronously
                    final int syncBalance = account.getBalance().intValue();
                    if (syncBalance <= 0) return;

                    int amountToWithdraw = 0;
                    final List<ItemStack> itemsToGive = new ArrayList<>();

                    // Create a dummy inventory to test space limitations safely
                    final Inventory dummyInv = this.plugin.getServer().createInventory(null, 36);
                    dummyInv.setContents(player.getInventory().getStorageContents());

                    int remainingBalance = syncBalance;

                    for (final Map.Entry<String, Integer> entry : sortedItems) {
                        if (remainingBalance <= 0) break;

                        final Material material = Material.matchMaterial(entry.getKey());
                        if (material == null) continue;

                        final int itemValue = entry.getValue();
                        if (itemValue <= 0) continue;

                        final int maxAffordable = remainingBalance / itemValue;
                        if (maxAffordable == 0) continue;

                        int itemsLeftToTest = maxAffordable;
                        int itemsAddedToDummy = 0;

                        // Break affordable items into stack sizes and test fit them
                        while (itemsLeftToTest > 0) {
                            final int stackSize = Math.min(itemsLeftToTest, material.getMaxStackSize());
                            final ItemStack testStack = new ItemStack(material, stackSize);

                            final Map<Integer, ItemStack> leftovers = dummyInv.addItem(testStack);

                            if (leftovers.isEmpty()) {
                                // Completely fit
                                itemsAddedToDummy += stackSize;
                                itemsToGive.add(testStack.clone());
                                itemsLeftToTest -= stackSize;
                            } else {
                                // Partially fit or didn't fit at all
                                final int leftoverAmount = leftovers.get(0).getAmount();
                                final int successfullyAdded = stackSize - leftoverAmount;

                                if (successfullyAdded > 0) {
                                    itemsAddedToDummy += successfullyAdded;
                                    itemsToGive.add(new ItemStack(material, successfullyAdded));
                                }
                                break; // No more space for this specific item type in the inventory
                            }
                        }

                        // Update tracking variables based on what actually fit
                        final int valueTaken = itemsAddedToDummy * itemValue;
                        amountToWithdraw += valueTaken;
                        remainingBalance -= valueTaken;
                    }

                    // If we calculated items to give, process the transaction
                    if (amountToWithdraw > 0 && !itemsToGive.isEmpty()) {
                        final BigDecimal decimalAmount = BigDecimal.valueOf(amountToWithdraw);
                        final boolean success = account.subtractBalance(decimalAmount, Transactor.SERVER);

                        if (success) {
                            for (final ItemStack stack : itemsToGive) {
                                player.getInventory().addItem(stack);
                            }
                        } else {
                            this.plugin.getComponentLogger().error(
                                    "Auto withdraw failed during balance subtraction for player '{}'",
                                    player.getName()
                            );
                        }
                    }
                });
            }, () -> {
                // No account found
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-account-not-found",
                        this.plugin.getMiniMessage()
                ));
            });
        }
    }
}