package com.spektrsoyuz.economy.command.suggest;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.spektrsoyuz.economy.EconomyPlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;

// Suggestion provider class for player accounts
@RequiredArgsConstructor
public class PlayerAccountSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

    private final EconomyPlugin plugin;

    @Override
    public CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        final CommandSender sender = ctx.getSource().getSender();

        // Suggest account names
        this.plugin.getAccountController().getAccounts().values().stream()
                .filter(account -> account.getName().toLowerCase().startsWith(builder.getRemainingLowerCase()))
                .filter(account -> this.plugin.getServer().getOfflinePlayer(account.getId()).hasPlayedBefore())
                .filter(account -> !(sender.getName().equalsIgnoreCase(account.getName())))
                .forEach(account -> builder.suggest(account.getName()));

        return builder.buildFuture();
    }
}
