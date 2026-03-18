package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

    // Executes the command with the 'all' argument
    private int executeAll(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-sender-not-player",
                    this.plugin.getMiniMessage()
            ));
            return 0;
        }

        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();

        return this.plugin.getAccountController().getAccount(player).map(account -> {
            final BigDecimal balance = account.getBalance();

            // Check account balance
            if (balance.compareTo(BigDecimal.ONE) < 0) {
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-not-enough-balance",
                        this.plugin.getMiniMessage(),
                        Placeholder.parsed("currency", config.getNamePlural())
                ));
                return 0;
            }

            // Process transaction
            return this.plugin.getEconomyController().handleWithdrawal(player, balance.intValue(), config);
        }).orElseGet(() -> {
            // Account not found
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-account-not-found",
                    this.plugin.getMiniMessage()
            ));
            return 0;
        });
    }

    // Executes the command
    private int execute(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-sender-not-player",
                    this.plugin.getMiniMessage()
            ));
            return 0;
        }

        final int amount = ctx.getArgument("amount", Integer.class);
        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();

        // Process transaction
        return this.plugin.getEconomyController().handleWithdrawal(player, amount, currencyConfig);
    }

}
