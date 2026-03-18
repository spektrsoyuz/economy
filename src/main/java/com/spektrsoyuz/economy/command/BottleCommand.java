package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.model.account.Transactor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.entity.PlayerGiveResult;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;

import java.math.BigDecimal;

/**
 * Model class for the /bottle command.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
@SuppressWarnings("UnstableApiUsage")
public final class BottleCommand {

    private final EconomyPlugin plugin;

    /**
     * Registers the command.
     *
     * @param registrar The command registrar.
     */
    public void register(final Commands registrar) {
        final var command = Commands.literal("bottle")
                .requires(s -> s.getSender().hasPermission(Constants.PERMISSION_COMMAND_BOTTLE))
                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(this::execute))
                .build();

        registrar.register(command, "Store XP in a bottle");
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
            // Account found for player
            final int amount = ctx.getArgument("amount", Integer.class);
            final BigDecimal amountDecimal = BigDecimal.valueOf(amount);
            final String currency = EconomyUtils.format(this.plugin, amountDecimal);

            // Subtract amount from player account
            account.subtractBalance(amountDecimal, Transactor.SERVER);

            // Give XP bottles to player
            final ItemStack itemStack = ItemType.EXPERIENCE_BOTTLE.createItemStack(amount);

            final PlayerGiveResult result = player.give(itemStack);
            if (!result.leftovers().isEmpty()) {
                for (final ItemStack droppedItem : result.leftovers()) {
                    player.getWorld().dropItem(player.getLocation(), droppedItem);
                }
            }

            // Send message to player
            player.sendMessage(this.plugin.getConfigController().getMessage(
                    "economy-exp-store",
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
