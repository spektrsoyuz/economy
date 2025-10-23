package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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

import java.math.BigDecimal;
import java.util.List;

// Command class for the /economy command
@RequiredArgsConstructor
public final class EconomyCommand {

    private final EconomyPlugin plugin;

    // Registers the command
    public void register(final Commands registrar) {
        final var command = Commands.literal("economy")
                .requires(s -> s.getSender().hasPermission(EconomyUtils.PERMISSION_COMMAND_ECONOMY))
                .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(new AccountSuggestionProvider(this.plugin))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(this::executeAdd))))
                .then(Commands.literal("subtract")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(new AccountSuggestionProvider(this.plugin))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(this::executeSubtract))))
                .build();

        registrar.register(command, "Manage the economy", List.of("eco"));
    }

    // Executes the 'add' sub-command
    private int executeAdd(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        final String accountName = ctx.getArgument("name", String.class);
        final int amount = ctx.getArgument("amount", Integer.class);
        final BigDecimal amountBD = BigDecimal.valueOf(amount);

        // Check if target account exists
        return this.plugin.getAccountController().getAccount(accountName).map(account -> {
            // Perform the transaction
            account.addBalance(amountBD);

            // Send success messages to sender
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-economy-add",
                    Placeholder.parsed("name", accountName),
                    Placeholder.parsed("symbol", this.plugin.getConfigController().getCurrencyConfig().getSymbol()),
                    Placeholder.parsed("amount", String.valueOf(amount)),
                    Placeholder.parsed("currency", EconomyUtils.format(this.plugin, account.getBalance()))));

            return Command.SINGLE_SUCCESS;
        }).orElseGet(() -> {

            // Check if transaction was successful
            if (this.plugin.getAccountController().getAccount(accountName).isEmpty()) {
                sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
            }
            return 0;
        });
    }

    // Executes the 'subtract' sub-command
    private int executeSubtract(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        final String accountName = ctx.getArgument("name", String.class);
        final int amount = ctx.getArgument("amount", Integer.class);
        final BigDecimal amountBD = BigDecimal.valueOf(amount);

        // Check if target account exists
        return this.plugin.getAccountController().getAccount(accountName).map(account -> {
            // Perform the transaction
            account.subtractBalance(amountBD);

            // Send success messages to sender
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-economy-subtract",
                    Placeholder.parsed("name", accountName),
                    Placeholder.parsed("symbol", this.plugin.getConfigController().getCurrencyConfig().getSymbol()),
                    Placeholder.parsed("amount", String.valueOf(amount)),
                    Placeholder.parsed("currency", EconomyUtils.format(this.plugin, account.getBalance()))));

            return Command.SINGLE_SUCCESS;
        }).orElseGet(() -> {

            // Check if transaction was successful
            if (this.plugin.getAccountController().getAccount(accountName).isEmpty()) {
                sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
            }
            return 0;
        });
    }
}
