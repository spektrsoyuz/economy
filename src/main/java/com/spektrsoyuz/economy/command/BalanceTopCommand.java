package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

// Command class for the /balancetop command
@RequiredArgsConstructor
public final class BalanceTopCommand {

    private final EconomyPlugin plugin;

    // Registers the command
    public void register(final Commands registrar) {
        final var command = Commands.literal("balancetop")
                .requires(stack -> stack.getSender().hasPermission(Constants.PERMISSION_COMMAND_BALANCE))
                .executes(this::execute)
                .build();

        registrar.register(command, "View the account leaderboard", List.of("baltop"));
    }

    // Executes the command
    private int execute(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        final List<Component> components = this.plugin.getAccountController().getTopAccounts()
                .stream()
                .map(account -> {
                    final String name = account.getFormattedName();
                    final String balance = account.getBalance().toString();
                    final String currency = EconomyUtils.format(this.plugin, account.getBalance());

                    // Create component for account
                    return this.plugin.getConfigController().getMessage("command-balancetop-body",
                            Placeholder.parsed("name", name),
                            Placeholder.parsed("balance", balance),
                            Placeholder.parsed("currency", currency)
                    );
                })
                .toList();

        // Build final message
        final List<Component> finalComponents = new ArrayList<>();
        final int limit = Math.min(components.size(), 10);
        final List<Component> reducedComponents = components.subList(0, limit);

        finalComponents.add(this.plugin.getConfigController().getMessage("command-balancetop-header",
                Placeholder.parsed("count", String.valueOf(reducedComponents.size()))));
        finalComponents.addAll(reducedComponents);
        finalComponents.add(this.plugin.getConfigController().getMessage("command-balancetop-footer"));

        // Send message to sender
        final Component message = Component.join(JoinConfiguration.newlines(), finalComponents);
        sender.sendMessage(message);

        return Command.SINGLE_SUCCESS;
    }
}
