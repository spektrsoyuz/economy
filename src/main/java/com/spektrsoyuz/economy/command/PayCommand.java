package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.command.suggest.AccountSuggestionProvider;
import com.spektrsoyuz.economy.model.account.Account;
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

    /**
     * Registers the command using the Paper Command API.
     *
     * @param registrar Paper Command API registrar
     */
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

    /**
     * Executes the command and returns the success status.
     *
     * @param ctx command context
     * @return success status
     */
    private int execute(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage("error-sender-not-player"));
            return 0;
        }

        final Optional<Account> optional = this.plugin.getAccountController().getAccount(player);

        // Check if account exists
        if (optional.isPresent()) {
            final Account account = optional.get();
            final String targetName = ctx.getArgument("name", String.class);
            final Optional<Account> targetOptional = this.plugin.getAccountController().getAccount(targetName);

            if (targetOptional.isPresent()) {
                final Account targetAccount = targetOptional.get();
                final int amount = ctx.getArgument("amount", Integer.class);

                // Check if account balance is less than amount
                if (account.getBalance().compareTo(BigDecimal.valueOf(amount)) < 0) {
                    sender.sendMessage(this.plugin.getConfigController().getMessage("error-not-enough-balance",
                            Placeholder.parsed("currency_plural", this.plugin.getConfigController().getCurrencyConfig().getNamePlural())));
                    return 0;
                }

                account.subtractBalance(BigDecimal.valueOf(amount));
                targetAccount.addBalance(BigDecimal.valueOf(amount));

                // Send success messages
                sender.sendMessage(this.plugin.getConfigController().getMessage("command-pay-send",
                        Placeholder.parsed("currency_name", this.plugin.getConfigController().getCurrencyConfig().getName()),
                        Placeholder.parsed("amount", String.valueOf(amount)),
                        Placeholder.parsed("player", targetName)));

                final Player targetPlayer = this.plugin.getServer().getPlayer(targetName);

                if (targetPlayer != null) {
                    targetPlayer.sendMessage(this.plugin.getConfigController().getMessage("command-pay-receive",
                            Placeholder.parsed("currency_name", this.plugin.getConfigController().getCurrencyConfig().getName()),
                            Placeholder.parsed("amount", String.valueOf(amount)),
                            Placeholder.parsed("player", player.getName())));
                }

                return Command.SINGLE_SUCCESS;
            } else {
                // Account does not exist
                sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
                return 0;
            }
        } else {
            // Account does not exist
            sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found"));
            return 0;
        }
    }
}
