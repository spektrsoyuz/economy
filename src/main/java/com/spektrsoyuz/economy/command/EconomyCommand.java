package com.spektrsoyuz.economy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.EconomyUtils;
import com.spektrsoyuz.economy.command.suggest.AccountSuggestionProvider;
import com.spektrsoyuz.economy.model.Account;
import com.spektrsoyuz.economy.model.Transactor;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Command class for the /economy command
@RequiredArgsConstructor
public final class EconomyCommand {

    private final EconomyPlugin plugin;

    // Registers the command
    public void register(final Commands registrar) {
        // /economy add <name> <amount>
        final var add = Commands.literal("add")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(new AccountSuggestionProvider(this.plugin))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(this::add)));

        // /economy create <name>
        final var create = Commands.literal("create")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(this::create));

        // /economy delete <name>
        final var delete = Commands.literal("delete")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(new AccountSuggestionProvider(this.plugin))
                        .executes(this::delete));

        // /economy freeze <name>
        final var freeze = Commands.literal("freeze")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(new AccountSuggestionProvider(this.plugin))
                        .executes(this::freeze));

        // /economy reload
        final var reload = Commands.literal("reload")
                .executes(this::reload);

        // /economy reset <name>
        final var reset = Commands.literal("reset")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(new AccountSuggestionProvider(this.plugin))
                        .executes(this::reset));

        // /economy set <name> <amount>
        final var set = Commands.literal("set")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(new AccountSuggestionProvider(this.plugin))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(this::set)));

        // /economy subtract <name> <amount>
        final var subtract = Commands.literal("subtract")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(new AccountSuggestionProvider(this.plugin))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                .executes(this::subtract)));

        // /economy unfreeze <name>
        final var unfreeze = Commands.literal("unfreeze")
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(new AccountSuggestionProvider(this.plugin))
                        .executes(this::unfreeze));

        // /economy update
        final var update = Commands.literal("update")
                .executes(this::update);

        // /economy ...
        final var command = Commands.literal("economy")
                .requires(s -> s.getSender().hasPermission(Constants.PERMISSION_ADMIN))
                .then(add)
                .then(create)
                .then(delete)
                .then(freeze)
                .then(reload)
                .then(reset)
                .then(set)
                .then(subtract)
                .then(unfreeze)
                .then(update)
                .build();

        registrar.register(command, "Manage the economy", List.of("eco"));
    }

    // Executes the 'add' sub-command
    private int add(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String accountName = ctx.getArgument("name", String.class);
        final int amount = IntegerArgumentType.getInteger(ctx, "amount");
        final BigDecimal amountBD = BigDecimal.valueOf(amount);

        // Check if account exists
        this.plugin.getAccountController().getAccount(accountName).ifPresentOrElse(account -> {
            final String symbol = this.plugin.getConfigController().getCurrencyConfig().getSymbol();
            final String currency = EconomyUtils.format(this.plugin, account.getBalance());

            // Perform the transaction
            account.addBalance(amountBD, Transactor.SERVER);

            // Send message to sender
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-economy-add",
                    Placeholder.parsed("name", accountName),
                    Placeholder.parsed("symbol", symbol),
                    Placeholder.parsed("amount", String.valueOf(amount)),
                    Placeholder.parsed("currency", currency)
            ));
        }, () -> sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found")));

        return Command.SINGLE_SUCCESS;
    }

    // Executes the 'create' sub-command
    private int create(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String accountName = ctx.getArgument("name", String.class);

        final Player player = this.plugin.getServer().getPlayer(accountName);
        final UUID uuid = player != null
                ? player.getUniqueId()
                : UUID.randomUUID();

        // Create account
        final Account account = this.plugin.getAccountController().createAccount(uuid, accountName);

        if (player != null) {
            this.plugin.getAccountController().addPlayerAccount(account);
        }

        // Send message to sender
        sender.sendMessage(this.plugin.getConfigController().getMessage("command-economy-create",
                Placeholder.parsed("name", accountName)
        ));
        return Command.SINGLE_SUCCESS;
    }

    // Executes the 'delete' sub-command
    private int delete(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String accountName = ctx.getArgument("name", String.class);

        // Check if account exists
        this.plugin.getAccountController().getAccount(accountName).ifPresentOrElse(account -> {
            // Delete account
            this.plugin.getAccountController().deleteAccount(account.getId());

            // Send message to sender
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-economy-delete",
                    Placeholder.parsed("name", accountName)
            ));
        }, () -> sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found")));

        return Command.SINGLE_SUCCESS;
    }

    // Executes the 'freeze' sub-command
    private int freeze(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String accountName = ctx.getArgument("name", String.class);

        // Check if account exists
        this.plugin.getAccountController().getAccount(accountName).ifPresentOrElse(account -> {
            // Freeze the account
            account.freeze();

            // Send message to sender
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-economy-freeze",
                    Placeholder.parsed("name", accountName)
            ));
        }, () -> sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found")));

        return Command.SINGLE_SUCCESS;
    }

    // Executes the 'reload' sub-command
    private int reload(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Reload config
        final boolean success = this.plugin.getConfigController().initialize();

        // Send message to sender
        sender.sendMessage(this.plugin.getConfigController().getMessage(success
                ? "command-economy-reload-success"
                : "command-economy-reload-fail"
        ));

        return Command.SINGLE_SUCCESS;
    }

    // Executes the 'reset' sub-command
    private int reset(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String accountName = ctx.getArgument("name", String.class);

        // Check if account exists
        this.plugin.getAccountController().getAccount(accountName).ifPresentOrElse(account -> {
            // Reset the account
            account.setBalance(BigDecimal.ZERO, Transactor.SERVER);
            account.setFrozen(false);

            // Send message to sender
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-economy-reset",
                    Placeholder.parsed("name", accountName)
            ));
        }, () -> sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found")));

        return Command.SINGLE_SUCCESS;
    }

    // Executes the 'set' sub-command
    private int set(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String accountName = ctx.getArgument("name", String.class);
        final int amount = IntegerArgumentType.getInteger(ctx, "amount");
        final BigDecimal amountBD = BigDecimal.valueOf(amount);

        // Check if account exists
        this.plugin.getAccountController().getAccount(accountName).ifPresentOrElse(account -> {
            final String symbol = this.plugin.getConfigController().getCurrencyConfig().getSymbol();
            final String currency = EconomyUtils.format(this.plugin, account.getBalance());

            // Perform the transaction
            account.setBalance(amountBD, Transactor.SERVER);

            // Send message to sender
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-economy-set",
                    Placeholder.parsed("name", accountName),
                    Placeholder.parsed("symbol", symbol),
                    Placeholder.parsed("amount", String.valueOf(amount)),
                    Placeholder.parsed("currency", currency)
            ));
        }, () -> sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found")));

        return Command.SINGLE_SUCCESS;
    }

    // Executes the 'subtract' sub-command
    private int subtract(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String accountName = ctx.getArgument("name", String.class);
        final int amount = IntegerArgumentType.getInteger(ctx, "amount");
        final BigDecimal amountBD = BigDecimal.valueOf(amount);

        // Check if account exists
        this.plugin.getAccountController().getAccount(accountName).ifPresentOrElse(account -> {
            final String symbol = this.plugin.getConfigController().getCurrencyConfig().getSymbol();
            final String currency = EconomyUtils.format(this.plugin, account.getBalance());

            // Perform the transaction
            account.subtractBalance(amountBD, Transactor.SERVER);

            // Send message to sender
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-economy-subtract",
                    Placeholder.parsed("name", accountName),
                    Placeholder.parsed("symbol", symbol),
                    Placeholder.parsed("amount", String.valueOf(amount)),
                    Placeholder.parsed("currency", currency)
            ));
        }, () -> sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found")));

        return Command.SINGLE_SUCCESS;
    }

    // Executes the 'unfreeze' sub-command
    private int unfreeze(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        final String accountName = ctx.getArgument("name", String.class);

        // Check if account exists
        this.plugin.getAccountController().getAccount(accountName).ifPresentOrElse(account -> {
            // Unfreeze the account
            account.unfreeze();

            // Send message to sender
            sender.sendMessage(this.plugin.getConfigController().getMessage("command-economy-unfreeze",
                    Placeholder.parsed("name", accountName)
            ));
        }, () -> sender.sendMessage(this.plugin.getConfigController().getMessage("error-account-not-found")));

        return Command.SINGLE_SUCCESS;
    }

    // Executes the 'update' sub-command
    private int update(final CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();

        // Update the config
        this.plugin.getConfigController().reset(Constants.CONFIG_MESSAGES);
        final boolean success = this.plugin.getConfigController().initialize();

        // Send message to sender
        sender.sendMessage(this.plugin.getConfigController().getMessage(success
                ? "command-economy-update-success"
                : "command-economy-update-fail"
        ));

        return Command.SINGLE_SUCCESS;
    }
}
