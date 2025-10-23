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
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Optional;

// Command class for the /pay command
@RequiredArgsConstructor
public final class PayCommand {

    private final EconomyPlugin plugin;

    // Registers the command
    public void register(final Commands registrar) {
        final var command = Commands.literal("pay")
                .requires(s -> s.getSender().hasPermission(EconomyUtils.PERMISSION_COMMAND_PAY))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(new AccountSuggestionProvider(this.plugin))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(this::execute)))
                .build();

        registrar.register(command, "Give currency to an account");
    }

    // Executes the command
    private int execute(final CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack source = ctx.getSource();
        final CommandSender sender = source.getSender();

        // Check if sender is a player, and use an early return if not.
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage("error-sender-not-player"));
            return 0;
        }

        final String currencyName = this.plugin.getConfigController().getCurrencyConfig().getName();
        final String currencyPlural = this.plugin.getConfigController().getCurrencyConfig().getNamePlural();
        final String targetName = ctx.getArgument("name", String.class);
        final int amount = ctx.getArgument("amount", Integer.class);
        final BigDecimal amountBD = BigDecimal.valueOf(amount);

        // Check if player account exists
        return this.plugin.getAccountController().getAccount(player)
                .flatMap(account -> {
                    // Check if account balance is less than amount
                    if (account.getBalance().compareTo(amountBD) < 0) {
                        sender.sendMessage(this.plugin.getConfigController().getMessage("error-not-enough-balance",
                                Placeholder.parsed("currency_plural", currencyPlural)));
                        return Optional.empty();
                    }

                    // Check if target account exists
                    return this.plugin.getAccountController().getAccount(targetName)
                            .map(targetAccount -> {
                                // Perform the transaction
                                account.subtractBalance(amountBD);
                                targetAccount.addBalance(amountBD);

                                // Send success messages to sender
                                sender.sendMessage(this.plugin.getConfigController().getMessage("command-pay-send",
                                        Placeholder.parsed("currency_name", currencyName),
                                        Placeholder.parsed("amount", String.valueOf(amount)),
                                        Placeholder.parsed("player", targetName)));

                                // Send success message to target
                                final Player targetPlayer = this.plugin.getServer().getPlayer(targetName);
                                if (targetPlayer != null) {
                                    targetPlayer.sendMessage(this.plugin.getConfigController().getMessage("command-pay-receive",
                                            Placeholder.parsed("currency_name", currencyName),
                                            Placeholder.parsed("amount", String.valueOf(amount)),
                                            Placeholder.parsed("player", player.getName())));
                                }

                                return Command.SINGLE_SUCCESS;
                            });
                })
                .orElseGet(() -> {
                    // Check if transaction was successful
                    if (this.plugin.getAccountController().getAccount(targetName).isEmpty()) {
                        sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
                    }
                    return 0;
                });
    }
}
