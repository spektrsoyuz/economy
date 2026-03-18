package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
                .then(Commands.literal("auto")
                        .requires(s -> s.getSender().hasPermission(Constants.PERMISSION_COMMAND_DEPOSIT_AUTO))
                        .executes(this::executeAuto))
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
            sender.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-sender-not-player",
                    this.plugin.getMiniMessage()
            ));
            return 0;
        }

        final CurrencyConfig config = this.plugin.getConfigController().getCurrencyConfig();
        int totalValueInInventory = 0;

        // Calculate total physical currency in inventory
        for (final ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;

            int valuePerItem = config.getItemValue(item.getType());
            if (valuePerItem > 0) {
                totalValueInInventory += (valuePerItem * item.getAmount());
            }
        }

        // Check if player has enough items to deposit
        if (totalValueInInventory <= 0) {
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-not-enough-balance",
                    this.plugin.getMiniMessage(),
                    Placeholder.parsed("currency", config.getNamePlural())
            ));

            EconomyUtils.playErrorSound(player);
            return 0;
        }

        // Process transaction
        return this.plugin.getEconomyController().handleDeposit(player, totalValueInInventory, config);
    }

    // Executes the command with the 'auto' argument
    private int executeAuto(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Check if sender is a player
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-sender-not-player",
                    this.plugin.getMiniMessage()
            ));
            return 0;
        }

        this.plugin.getAccountController().getAccount(player).ifPresentOrElse(account -> {
            // Account found
            final boolean auto = account.isAutoDeposit();

            account.setAutoDeposit(!auto);

            final String messageKey = auto
                    ? "command-deposit-auto-disable"
                    : "command-deposit-auto-enable";

            player.sendMessage(this.plugin.getConfigController().getMessage(
                    messageKey,
                    this.plugin.getMiniMessage()
            ));
        }, () -> {
            // No account found
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    "error-account-not-found",
                    this.plugin.getMiniMessage()
            ));
        });

        return Command.SINGLE_SUCCESS;
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
        return this.plugin.getEconomyController().handleDeposit(player, amount, currencyConfig);
    }

}
