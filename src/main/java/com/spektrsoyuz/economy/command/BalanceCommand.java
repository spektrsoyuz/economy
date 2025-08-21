package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.command.suggest.AccountSuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.List;

// Command class for the /balance command
@RequiredArgsConstructor
public final class BalanceCommand {

    private final EconomyPlugin plugin;

    /**
     * Registers the command using the Paper Command API.
     *
     * @param registrar Paper Command API registrar
     */
    public void register(final Commands registrar) {
        final var command = Commands.literal("balance")
                .requires(s -> s.getSender().hasPermission(EconomyUtils.PERMISSION_COMMAND_BALANCE))
                .then(Commands.argument("name", StringArgumentType.word())
                        .requires(s -> s.getSender().hasPermission(EconomyUtils.PERMISSION_COMMAND_BALANCE_OTHER))
                        .suggests(new AccountSuggestionProvider(this.plugin))
                        .executes(this::executeOther))
                .executes(this::executeSelf)
                .build();

        registrar.register(command, "View your account balance", List.of("bal", "money"));
    }

    /**
     * Executes the command for the sender and returns the success status.
     *
     * @param ctx command context
     * @return success status
     */
    private int executeSelf(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage("error-sender-not-player"));
            return 0;
        }

        return this.plugin.getAccountController().getAccount(player)
                .map(account -> {
                    final DecimalFormat format = new DecimalFormat("0.#");
                    final String balance = format.format(account.getBalance().doubleValue());

                    final String symbol = this.plugin.getConfigController().getCurrencyConfig().getSymbol();
                    final String currencyFormat = account.getBalance().doubleValue() > 1
                            ? this.plugin.getConfigController().getCurrencyConfig().getNamePlural()
                            : this.plugin.getConfigController().getCurrencyConfig().getNameSingular();

                    // Send message to the sender
                    sender.sendMessage(this.plugin.getConfigController().getMessage("command-balance",
                            Placeholder.parsed("symbol", symbol),
                            Placeholder.parsed("amount", balance),
                            Placeholder.parsed("currency", currencyFormat)));

                    return Command.SINGLE_SUCCESS;
                })
                .orElseGet(() -> {
                    // Account does not exist
                    sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
                    return 0;
                });
    }

    /**
     * Executes the command for a target account and returns the success status.
     *
     * @param ctx command context
     * @return success status
     */
    private int executeOther(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String name = ctx.getArgument("name", String.class);

        return this.plugin.getAccountController().getAccount(name)
                .map(account -> {
                    final DecimalFormat format = new DecimalFormat("0.#");
                    final String balance = format.format(account.getBalance().doubleValue());

                    final String symbol = this.plugin.getConfigController().getCurrencyConfig().getSymbol();
                    final String currencyFormat = account.getBalance().doubleValue() > 1
                            ? this.plugin.getConfigController().getCurrencyConfig().getNamePlural()
                            : this.plugin.getConfigController().getCurrencyConfig().getNameSingular();

                    // Send message to the sender
                    sender.sendMessage(this.plugin.getConfigController().getMessage("command-balance-other",
                            Placeholder.parsed("name", name),
                            Placeholder.parsed("symbol", symbol),
                            Placeholder.parsed("amount", balance),
                            Placeholder.parsed("currency", currencyFormat)));

                    return Command.SINGLE_SUCCESS;
                })
                .orElseGet(() -> {
                    // Account does not exist
                    sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
                    return 0;
                });
    }
}
