package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.account.Transactor;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Model class for the /deposit command.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class DepositCommand {

    private final EconomyPlugin plugin;

    /**
     * Registers the command.
     *
     * @param registrar The command registrar.
     */
    public void register(final Commands registrar) {
        final var command = Commands.literal("deposit")
                .requires(s -> s.getSender().hasPermission(Constants.PERMISSION_COMMAND_DEPOSIT))
                .then(Commands.literal("all")
                        .executes(this::executeAll))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(this::execute))
                .build();

        registrar.register(command, "Deposit into your bank");
    }

    // Executes the command with the 'all' argument
    private int executeAll(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage("error-sender-not-player", this.plugin.getMiniMessage()));
            return 0;
        }

        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        int totalValueInInventory = 0;

        // Calculate total physical currency in inventory
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;

            int valuePerItem = config.getItemValue(item.getType());
            if (valuePerItem > 0) {
                totalValueInInventory += (valuePerItem * item.getAmount());
            }
        }

        // Check if player has enough items to deposit
        if (totalValueInInventory <= 0) {
            player.sendMessage(this.plugin.getConfigController().getMessage("error-not-enough-balance", this.plugin.getMiniMessage()));

            EconomyUtils.playErrorSound(player);
            return 0;
        }

        // Process transaction
        return this.handleTransaction(player, totalValueInInventory, config);
    }

    // Executes the command
    private int execute(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage("error-sender-not-player", this.plugin.getMiniMessage()));
            return 0;
        }

        final int amount = ctx.getArgument("amount", Integer.class);
        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();

        // Process transaction
        return this.handleTransaction(player, amount, currencyConfig);
    }

    // Handles the deposit transaction
    private int handleTransaction(final Player player, final int amount, final CurrencyConfig currencyConfig) {
        this.plugin.getAccountController().getAccount(player).ifPresentOrElse(account -> {
            // Calculate total value available in inventory
            long totalValueInInventory = 0;
            for (final ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType().isAir()) continue;
                int value = currencyConfig.getItemValue(item.getType());

                if (value > 0) {
                    totalValueInInventory += (long) value * item.getAmount();
                }
            }

            // Check if player has enough value
            if (totalValueInInventory < amount) {
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-not-enough-balance",
                        this.plugin.getMiniMessage()
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
                int val = currencyConfig.getItemValue(item.getType());

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
                        this.refundValue(player, currencyConfig, changeDue);
                    }
                }
            }

            // Update account balance
            final BigDecimal amountDecimal = BigDecimal.valueOf(amount);
            final boolean success = account.addBalance(amountDecimal, Transactor.SERVER);

            // Check if transaction failed
            if (!success) {
                // Refund items to play
                this.refundValue(player, currencyConfig, amount);
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-transaction-failed",
                        this.plugin.getMiniMessage()
                ));

                EconomyUtils.playErrorSound(player);
                return;
            }

            // Send message to player
            final String currencyFormatted = EconomyUtils.format(this.plugin, amountDecimal);
            final String messageKey = String.format("economy-%s-deposit", currencyConfig.getType().name().toLowerCase());

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

    // Refunds items to the player if the transaction fails
    private void refundValue(final Player player, final CurrencyConfig config, final int totalValue) {
        int remainingToRefund = totalValue;

        // Get currency items sorted by value descending
        final List<Map.Entry<String, Integer>> sortedItems = new ArrayList<>(config.getItems().entrySet());
        sortedItems.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        for (final Map.Entry<String, Integer> entry : sortedItems) {
            if (remainingToRefund <= 0) break;

            final Material material = Material.matchMaterial(entry.getKey());
            if (material == null) continue;

            final int itemValue = entry.getValue();
            final int countToGive = remainingToRefund / itemValue;

            if (countToGive > 0) {
                final ItemStack stack = new ItemStack(material, countToGive);

                // Give items to player
                player.getInventory().addItem(stack).values().forEach(leftover -> {
                    // Drop remaining items
                    player.getWorld().dropItem(player.getLocation(), leftover);
                });

                remainingToRefund %= itemValue;
            }
        }
    }

    // Model record for the value of an inventory slot
    private record SlotValue(int slot, int valuePerItem) {
    }

}
