package com.spektrsoyuz.economy.controller;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.Account;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

// Controller class for economy accounts
@Getter
@RequiredArgsConstructor
public final class AccountController {

    private final EconomyPlugin plugin;
    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();
    private final Map<UUID, Account> onlineAccounts = new ConcurrentHashMap<>();
    private final List<Account> topAccounts = new ArrayList<>();

    // Initializes the controller
    public void initialize() {
        this.plugin.getComponentLogger().info("Loading accounts into cache");

        // Load accounts
        this.loadAccounts().join();
        this.plugin.getComponentLogger().info("Loaded {} accounts into cache", this.accounts.size());
    }

    // Gets an account from the cache by its ID
    public Optional<Account> getAccount(final UUID id) {
        return Optional.ofNullable(this.accounts.get(id));
    }

    // Gets an account from the cache using a Bukkit player
    public Optional<Account> getAccount(final Player player) {
        return this.getAccount(player.getUniqueId());
    }

    // Gets an Account from the cache by its name
    public Optional<Account> getAccount(final String name) {
        for (final Account account : this.accounts.values()) {
            if (account.getName().equals(name)) return Optional.of(account);
        }
        return Optional.empty();
    }

    // Gets a player account from the cache by its ID
    public Optional<Account> getPlayerAccount(final UUID id) {
        return Optional.ofNullable(this.onlineAccounts.get(id));
    }

    // Gets a player account from the cache using a Bukkit player
    public Optional<Account> getPlayerAccount(final Player player) {
        return this.getPlayerAccount(player.getUniqueId());
    }

    // Gets a player account from the cache by its name
    public Optional<Account> getPlayerAccount(final String name) {
        for (final Account account : this.onlineAccounts.values()) {
            if (account.getName().equals(name)) return Optional.of(account);
        }
        return Optional.empty();
    }

    // Creates a new account and adds it to the cache
    public Account createAccount(final UUID id, final String name) {
        final double balance = this.plugin.getConfigController().getCurrencyConfig().getStartingBalance();
        final Account account = new Account(this.plugin, id, name, BigDecimal.valueOf(balance), false);

        this.accounts.put(id, account);
        return account;
    }

    // Deletes an account from the cache
    public boolean deleteAccount(final UUID id) {
        this.accounts.remove(id);

        this.plugin.getDataController().deleteAccount(id);
        return true;
    }

    // Load all accounts into the cache from the database
    private CompletableFuture<Void> loadAccounts() {
        return this.plugin.getDataController().queryAccounts().thenAccept(accounts -> {
            for (final Account account : accounts) {
                this.accounts.put(account.getId(), account);
            }
        });
    }

    // Sets the account name to a new name
    public boolean renameAccount(final UUID id, final String name) {
        final Account account = this.accounts.get(id);

        if (account != null) {
            return account.setName(name);
        }
        return false;
    }

    // Updates the top accounts
    public void updateTopAccounts(final List<Account> accounts) {
        this.topAccounts.clear();
        this.topAccounts.addAll(accounts);
    }

    // Adds a player account
    public void addPlayerAccount(final Account account) {
        this.onlineAccounts.put(account.getId(), account);
    }

    // Removes a player account
    public void removePlayerAccount(final Account account) {
        this.onlineAccounts.remove(account.getId());
    }
}
