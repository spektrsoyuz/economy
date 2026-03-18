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
import io.papermc.paper.entity.PlayerGiveResult;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

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

        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();
        int totalInInventory = 0;

        // Calculate total physical currency in inventory
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType().asItemType() == currencyConfig.getItem()) {
                totalInInventory += item.getAmount();
            }
        }

        if (totalInInventory <= 0) {
            player.sendMessage(this.plugin.getConfigController().getMessage("error-no-items-to-deposit", this.plugin.getMiniMessage()));
            return 0;
        }

        // Process transaction
        return this.handleTransaction(player, totalInInventory, currencyConfig);
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

            // Check if player has enough items
            if (!player.getInventory().containsAtLeast(currencyConfig.getItem().createItemStack(), amount)) {
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-not-enough-items",
                        this.plugin.getMiniMessage(),
                        Placeholder.parsed("required", String.valueOf(amount))
                ));
                return;
            }

            // Remove items from inventory
            int remainingToRemove = amount;
            final ItemStack[] contents = player.getInventory().getContents();

            for (int i = 0; i < contents.length; i++) {
                final ItemStack item = contents[i];
                if (item != null && item.getType().asItemType() == currencyConfig.getItem()) {
                    final int stackAmount = item.getAmount();

                    if (stackAmount <= remainingToRemove) {
                        remainingToRemove -= stackAmount;
                        player.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(stackAmount - remainingToRemove);
                        remainingToRemove = 0;
                    }
                }

                if (remainingToRemove <= 0) break;
            }

            // Add to account balance
            final BigDecimal amountDecimal = BigDecimal.valueOf(amount);
            boolean success = account.addBalance(amountDecimal, Transactor.SERVER);

            // Handle transaction failure
            if (!success) {
                this.refundItems(player, currencyConfig, amount);
                player.sendMessage(this.plugin.getConfigController().getMessage("error-transaction-failed", this.plugin.getMiniMessage()));

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
            // Account not found
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-account-not-found",
                    this.plugin.getMiniMessage()
            ));
        });

        return Command.SINGLE_SUCCESS;
    }

    // Refunds items to the player if the transaction fails
    private void refundItems(final Player player, final CurrencyConfig config, final int amount) {
        int remainingToRefund = amount;
        final int maxStack = config.getItem().createItemStack().getMaxStackSize();

        // Iterate over remaining items
        while (remainingToRefund > 0) {
            final int toGive = Math.min(remainingToRefund, maxStack);
            final ItemStack itemStack = config.getItem().createItemStack(toGive);

            // Give item to player
            final PlayerGiveResult result = player.give(itemStack);

            if (!result.leftovers().isEmpty()) {
                for (final ItemStack leftover : result.leftovers()) {
                    // Drop leftovers
                    player.getWorld().dropItem(player.getLocation(), leftover);
                }
            }

            remainingToRefund -= toGive;
        }
    }

}