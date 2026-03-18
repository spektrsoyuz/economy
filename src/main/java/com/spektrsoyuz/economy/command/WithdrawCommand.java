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
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;

/**
 * Model class for the /withdraw command.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class WithdrawCommand {

    private final EconomyPlugin plugin;

    /**
     * Registers the command.
     *
     * @param registrar The command registrar.
     */
    public void register(final Commands registrar) {
        final var command = Commands.literal("withdraw")
                .requires(s -> s.getSender().hasPermission(Constants.PERMISSION_COMMAND_WITHDRAW))
                .then(Commands.literal("all")
                        .executes(this::executeAll))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(this::execute))
                .build();

        registrar.register(command, "Withdraw from your bank");
    }

    // Executes the "withdraw all" logic
    private int executeAll(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage("error-sender-not-player", this.plugin.getMiniMessage()));
            return 0;
        }

        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();

        return this.plugin.getAccountController().getAccount(player).map(account -> {
            final BigDecimal balance = account.getBalance();

            // Check account balance
            if (balance.compareTo(BigDecimal.ONE) < 0) {
                player.sendMessage(this.plugin.getConfigController().getMessage("error-not-enough-balance", this.plugin.getMiniMessage()));
                return 0;
            }

            // Process transaction (handleTransaction handles value-based space checks)
            return this.handleTransaction(player, balance.intValue(), currencyConfig);
        }).orElseGet(() -> {
            // Account not found
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-account-not-found",
                    this.plugin.getMiniMessage()
            ));
            return 0;
        });
    }

    // Executes the command for a specific amount
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

    // Handles the withdrawal transaction
    private int handleTransaction(final Player player, final int amount, final CurrencyConfig config) {
        this.plugin.getAccountController().getAccount(player).ifPresentOrElse(account -> {
            final BigDecimal amountDecimal = BigDecimal.valueOf(amount);

            // Check account balance
            if (account.getBalance().compareTo(amountDecimal) < 0) {
                player.sendMessage(this.plugin.getConfigController().getMessage("error-not-enough-balance", this.plugin.getMiniMessage()));
                return;
            }

            // Check inventory space (value-based)
            if (this.calculateAvailableSpace(player, config) < amount) {
                player.sendMessage(this.plugin.getConfigController().getMessage("error-not-enough-inventory-space", this.plugin.getMiniMessage()));
                return;
            }

            // Subtract amount from player account
            boolean success = account.subtractBalance(amountDecimal, Transactor.SERVER);

            if (!success) {
                player.sendMessage(this.plugin.getConfigController().getMessage("error-transaction-failed", this.plugin.getMiniMessage()));
                EconomyUtils.playErrorSound(player);
                return;
            }

            // Handle item distribution
            EconomyUtils.distributeItems(player, config, amount);

            // Send message to player
            final String currencyFormatted = EconomyUtils.format(this.plugin, amountDecimal);
            final String messageKey = String.format("economy-%s-withdraw", config.getType().name().toLowerCase());

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

}
