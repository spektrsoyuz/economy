package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.command.suggest.PlayerAccountSuggestionProvider;
import com.spektrsoyuz.economy.model.account.Account;
import com.spektrsoyuz.economy.model.account.Transactor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

/**
 * Model class for the /pay command.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class PayCommand {

    private final EconomyPlugin plugin;

    /**
     * Registers the command.
     *
     * @param registrar The command registrar.
     */
    public void register(final Commands registrar) {
        final var command = Commands.literal("pay")
                .requires(s -> s.getSender().hasPermission(Constants.PERMISSION_COMMAND_PAY))
                .then(Commands.argument("player", ArgumentTypes.player())
                        .suggests(new PlayerAccountSuggestionProvider(this.plugin))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(this::execute)))
                .build();

        registrar.register(command, "Give currency to a player");
    }

    // Executes the command
    private int execute(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final var source = ctx.getSource();

        // Check if the sender is a player
        if (!(source.getSender() instanceof Player sender)) {
            this.sendMessage(source.getSender(), "error-sender-not-player");
            return 0;
        }

        final var targetResolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        final Player target = targetResolver.resolve(source).getFirst();
        final BigDecimal amount = BigDecimal.valueOf(ctx.getArgument("amount", Integer.class));

        // Check if the sender is the target
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            this.sendMessage(sender, "command-pay-self");
            return 0;
        }

        final var senderOpt = this.plugin.getAccountController().getPlayerAccount(sender);
        final var targetOpt = this.plugin.getAccountController().getPlayerAccount(target);

        // Check if accounts exist
        if (senderOpt.isEmpty() || targetOpt.isEmpty()) {
            this.sendMessage(sender, "error-account-not-found");
            return 0;
        }

        final Account senderAccount = senderOpt.get();
        final Account targetAccount = targetOpt.get();

        // Check if sender has enough balance
        if (senderAccount.getBalance().compareTo(amount) < 0) {
            this.sendMessage(
                    sender,
                    "error-not-enough-balance",
                    Placeholder.parsed("currency", EconomyUtils.format(this.plugin, amount))
            );
            return 0;
        }

        // Subtract amount from sender account
        if (!senderAccount.subtractBalance(amount, Transactor.PLAYER)) {
            this.handleError(sender);
            return 0;
        }

        // Add amount to target account
        if (!targetAccount.addBalance(amount, Transactor.PLAYER)) {
            // Rollback transaction if failed
            senderAccount.addBalance(amount, Transactor.PLAYER);
            this.handleError(sender);
            return 0;
        }

        // Notify player of transaction success
        final String currency = EconomyUtils.format(this.plugin, amount);

        this.sendMessage(
                sender,
                "command-pay-send",
                Placeholder.parsed("name", targetAccount.getDisplayName()),
                Placeholder.parsed("currency", currency)
        );

        this.sendMessage(
                target,
                "command-pay-receive",
                Placeholder.parsed("name", senderAccount.getDisplayName()),
                Placeholder.parsed("currency", currency)
        );

        sender.playSound(sender.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        return Command.SINGLE_SUCCESS;
    }

    // Handles a transaction error
    private void handleError(final Player player) {
        this.sendMessage(player, "error-transaction-failed");
        EconomyUtils.playErrorSound(player);
    }

    // Sends a message to a sender
    private void sendMessage(
            final CommandSender sender,
            final String key, final TagResolver... tags
    ) {
        sender.sendMessage(this.plugin.getConfigController().getMessage(
                key, this.plugin.getMiniMessage(), tags
        ));
    }

}