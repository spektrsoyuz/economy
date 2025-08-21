package com.spektrsoyuz.economy.controller;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.account.Account;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
        this.plugin.getComponentLogger().info("Loading accounts into cache");

        // Load accounts
        this.loadAccounts().join();
        this.plugin.getComponentLogger().info("Loaded {} accounts into cache", this.accounts.size());
    }

    /**
     * Gets an account from the cache by its ID.
     *
     * @param id account ID
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
     * @param name account name
     * @return an {@code Optional} containing the account if found,
     * otherwise an empty {@code Optional}
     */
    public Optional<Account> getAccount(final String name) {
        for (final Account account : this.accounts.values()) {
            if (account.getName().equals(name)) return Optional.of(account);
        }
        return Optional.empty();
    }

    /**
     * Creates a new account and adds it to the cache.
     *
     * @param id      account ID
     * @param name    account name
     * @param balance starting balance
     * @return true if successful, otherwise false
     */
    public boolean createAccount(final UUID id, final String name, final BigDecimal balance) {
        final Account account = new Account(this.plugin, id, name, balance);

        this.accounts.put(id, account);
        return true;
    }

    /**
     * Deletes an account from the cache.
     *
     * @param id account ID to delete
     * @return true if successful, otherwise false
     */
    public boolean deleteAccount(final UUID id) {
        this.accounts.remove(id);

        this.plugin.getDataController().deleteAccount(id);
        return true;
    }

    /**
     * Load all accounts into the cache from the database.
     *
     * @return A {@code CompletableFuture} with no result
     */
    private CompletableFuture<Void> loadAccounts() {
        return this.plugin.getDataController().queryAccounts().thenAccept(accounts -> {
            for (final Account account : accounts) {
                this.accounts.put(account.getId(), account);
            }
        });
    }

    /**
     * Sets the account name to a new name
     *
     * @param id   account ID
     * @param name new account name
     * @return true if successful, otherwise false
     */
    public boolean renameAccount(final UUID id, final String name) {
        final Account account = this.accounts.get(id);

        if (account != null) return account.setName(name);
        return false;
    }
}
