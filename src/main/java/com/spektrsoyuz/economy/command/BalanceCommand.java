package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.command.suggest.PlayerAccountSuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

// Command class for the /balance command
@RequiredArgsConstructor
public final class BalanceCommand {

    private final EconomyPlugin plugin;

    // Registers the command
    public void register(final Commands registrar) {
        final var command = Commands.literal("balance")
                .requires(s -> s.getSender().hasPermission(Constants.PERMISSION_COMMAND_BALANCE))
                .then(Commands.argument("name", StringArgumentType.word())
                        .requires(s -> s.getSender().hasPermission(Constants.PERMISSION_COMMAND_BALANCE_OTHER))
                        .suggests(new PlayerAccountSuggestionProvider(this.plugin))
                        .executes(this::executeTarget))
                .executes(this::execute)
                .build();

        registrar.register(command, "View your account balance", List.of("bal", "money"));
    }

    // Executes the command
    private int execute(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage("error-sender-not-player"));
            return 0;
        }

        this.plugin.getAccountController().getAccount(player).ifPresentOrElse(account -> {
            // Account found for player
            final String balance = account.getBalance().toString();
            final String symbol = this.plugin.getConfigController().getCurrencyConfig().getSymbol();
            final String currency = EconomyUtils.format(this.plugin, account.getBalance());

            // Send message to player
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-balance",
                    Placeholder.parsed("name", account.getFormattedName()),
                    Placeholder.parsed("symbol", symbol),
                    Placeholder.parsed("amount", balance),
                    Placeholder.parsed("currency", currency)
            ));
        }, () -> {
            // No account found
            player.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
        });

        return Command.SINGLE_SUCCESS;
    }

    // Executes the command for a target account
    private int executeTarget(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String accountName = ctx.getArgument("name", String.class);

        this.plugin.getAccountController().getAccount(accountName).ifPresentOrElse(account -> {
            // Account found for player
            final String balance = account.getBalance().toString();
            final String symbol = this.plugin.getConfigController().getCurrencyConfig().getSymbol();
            final String currency = EconomyUtils.format(this.plugin, account.getBalance());

            final boolean isPlayer = this.plugin.getServer().getOfflinePlayer(account.getId()).hasPlayedBefore();

            // Check if player is allowed to view balance
            if (!(isPlayer) && !(sender.hasPermission(Constants.PERMISSION_COMMAND_BALANCE_ALL))) {
                sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
                return;
            }

            // Send message to player
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-balance-other",
                    Placeholder.parsed("name", account.getFormattedName()),
                    Placeholder.parsed("symbol", symbol),
                    Placeholder.parsed("amount", balance),
                    Placeholder.parsed("currency", currency)
            ));
        }, () -> {
            // No account found
            sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
        });

        return Command.SINGLE_SUCCESS;
    }
}
