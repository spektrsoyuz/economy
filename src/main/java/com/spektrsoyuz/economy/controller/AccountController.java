package com.spektrsoyuz.economy.controller;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.account.Account;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Controller class for economy accounts
@Getter
@RequiredArgsConstructor
public final class AccountController {

    private final EconomyPlugin plugin;
    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();

    /**
     * Initializes the controller.
     */
    public void initialize() {

    }

    /**
     * Gets an account from the cache by its ID.
     *
     * @param id Account ID
     * @return an {@code Optional} containing the account if found,
     * otherwise an empty {@code Optional}
     */
    public Optional<Account> getAccount(final UUID id) {
        return Optional.ofNullable(this.accounts.get(id));
    }

    /**
     * Gets an account from the cache using a Bukkit player.
     *
     * @param player Bukkit player
     * @return an {@code Optional} containing the account if found,
     * otherwise an empty {@code Optional}
     */
    public Optional<Account> getAccount(final Player player) {
        return this.getAccount(player.getUniqueId());
    }

    /**
     * Gets an Account from the cache by its name.
     *
     * @param name Account name
     * @return an {@code Optional} containing the account if found,
     * otherwise an empty {@code Optional}
     */
    public Optional<Account> getAccount(final String name) {
        for (final Account account : this.accounts.values()) {
            if (account.getName().equals(name)) return Optional.of(account);
        }
        return Optional.empty();
    }
}
