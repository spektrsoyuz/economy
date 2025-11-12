package com.spektrsoyuz.economy.command.suggest;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.spektrsoyuz.economy.EconomyPlugin;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;

// Suggestion provider class for accounts
@RequiredArgsConstructor
public class AccountSuggestionProvider implements SuggestionProvider<CommandSourceStack> {

    private final EconomyPlugin plugin;

    @Override
    public CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        // Suggest account names
        this.plugin.getAccountController().getAccounts().values().stream()
                .filter(account -> account.getName().toLowerCase().startsWith(builder.getRemainingLowerCase()))
                .forEach(account -> builder.suggest(account.getName()));

        return builder.buildFuture();
    }
}
