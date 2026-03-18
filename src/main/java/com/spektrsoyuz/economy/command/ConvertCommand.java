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
import io.papermc.paper.entity.PlayerGiveResult;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class for the /convert command.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class ConvertCommand {

    private final EconomyPlugin plugin;

    /**
     * Registers the command.
     *
     * @param registrar The command registrar.
     */
    public void register(final Commands registrar) {
        final var command = Commands.literal("convert")
                .requires(s -> s.getSender().hasPermission(Constants.PERMISSION_COMMAND_CONVERT))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(this::execute))
                .build();

        final List<String> aliases = new ArrayList<>();
        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();

        switch (currencyConfig.getType()) {
            case EXP -> aliases.add("bottle");
            case ITEM -> aliases.add("withdraw");
        }

        registrar.register(command, "Convert a currency to an item", aliases);
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

        this.plugin.getAccountController().getAccount(player).ifPresentOrElse(account -> {
            // Account found
            final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();
            final int amount = ctx.getArgument("amount", Integer.class);
            final BigDecimal amountDecimal = BigDecimal.valueOf(amount);
            final String currency = EconomyUtils.format(this.plugin, amountDecimal);
            final int maxStackSize = 64;

            if (account.getBalance().compareTo(amountDecimal) < 0) {
                // Not enough balance
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "not-enough-balance",
                        this.plugin.getMiniMessage()
                ));
                return;
            }

            // Calculate inventory space
            int totalSpaceAvailable = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item == null || item.getType().isAir()) {
                    totalSpaceAvailable += maxStackSize;
                } else if (item.getType().asItemType() == currencyConfig.getItem()) {
                    totalSpaceAvailable += (maxStackSize - item.getAmount());
                }
            }

            if (totalSpaceAvailable < amount) {
                // Not enough inventory space
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-not-enough-inventory-space",
                        this.plugin.getMiniMessage()
                ));
                return;
            }

            // Subtract amount from player account
            boolean success = account.subtractBalance(amountDecimal, Transactor.SERVER);

            if (!success) {
                // Transaction failed
                player.sendMessage(this.plugin.getConfigController().getMessage(
                        "error-transaction-failed",
                        this.plugin.getMiniMessage()
                ));
                EconomyUtils.playErrorSound(player);
                return;
            }

            // Handle item distribution
            int remainingToGive = amount;
            while (remainingToGive > 0) {
                final int currentStackSize = Math.min(remainingToGive, maxStackSize);
                final ItemStack itemStack = currencyConfig.getItem().createItemStack(currentStackSize);

                // Give item to the player
                final PlayerGiveResult result = player.give(itemStack);
                if (!result.leftovers().isEmpty()) {
                    for (final ItemStack droppedItem : result.leftovers()) {
                        player.getWorld().dropItem(player.getLocation(), droppedItem);
                    }
                }

                remainingToGive -= currentStackSize;
            }

            final String messageKey = String.format("economy-%s-store", currencyConfig.getType().name().toLowerCase());

            // Send success message to player
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    messageKey,
                    this.plugin.getMiniMessage(),
                    Placeholder.parsed("amount", String.valueOf(amount)),
                    Placeholder.parsed("currency", currency)
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

}
