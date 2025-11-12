package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.command.suggest.PlayerAccountSuggestionProvider;
import com.spektrsoyuz.economy.model.Transactor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
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
                .requires(s -> s.getSender().hasPermission(Constants.PERMISSION_COMMAND_PAY))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .suggests(new PlayerAccountSuggestionProvider(this.plugin))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(this::execute)))
                .build();

        registrar.register(command, "Give currency to a player");
    }

    // Executes the command
    private int execute(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final CommandSender sender = ctx.getSource().getSender();

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage("error-sender-not-player"));
            return 0;
        }

        final String currencyPlural = this.plugin.getConfigController().getCurrencyConfig().getNamePlural();
        final PlayerSelectorArgumentResolver targetResolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        final Player targetPlayer = targetResolver.resolve(ctx.getSource()).getFirst();
        final int amount = ctx.getArgument("amount", Integer.class);
        final BigDecimal amountBD = BigDecimal.valueOf(amount);

        // Check if target is player
        if (player.getName().equals(targetPlayer.getName())) {
            player.sendMessage(this.plugin.getConfigController().getMessage("command-pay-self"));
            return 0;
        }

        // Check if player account exists
        return this.plugin.getAccountController().getPlayerAccount(player)
                .flatMap(account -> {
                    // Check if account balance is less than amount
                    if (account.getBalance().compareTo(amountBD) < 0) {
                        player.sendMessage(this.plugin.getConfigController().getMessage("error-not-enough-balance",
                                Placeholder.parsed("currency", currencyPlural)));
                        return Optional.empty();
                    }

                    // Check if target account exists
                    return this.plugin.getAccountController().getPlayerAccount(targetPlayer)
                            .map(targetAccount -> {
                                // Perform the transaction
                                account.subtractBalance(amountBD, Transactor.PLAYER);
                                targetAccount.addBalance(amountBD, Transactor.PLAYER);

                                final String name = account.getFormattedName();
                                final String symbol = this.plugin.getConfigController().getCurrencyConfig().getSymbol();
                                final String currency = EconomyUtils.format(this.plugin, amountBD);

                                // Send success messages to sender
                                player.sendMessage(this.plugin.getConfigController().getMessage("command-pay-send",
                                        Placeholder.parsed("name", name),
                                        Placeholder.parsed("symbol", symbol),
                                        Placeholder.parsed("amount", amountBD.toString()),
                                        Placeholder.parsed("currency", currency)));

                                // Send success message to target
                                targetPlayer.sendMessage(this.plugin.getConfigController().getMessage("command-pay-receive",
                                        Placeholder.parsed("name", name),
                                        Placeholder.parsed("symbol", symbol),
                                        Placeholder.parsed("amount", amountBD.toString()),
                                        Placeholder.parsed("currency", currency)));

                                return Command.SINGLE_SUCCESS;
                            });
                })
                .orElseGet(() -> {
                    // Check if transaction was successful
                    if (this.plugin.getAccountController().getPlayerAccount(targetPlayer).isEmpty()) {
                        player.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
                    }
                    return 0;
                });
    }
}
