package com.spektrsoyuz.economy.controller;

import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.account.Account;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller class for economy accounts.
 *
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public final class AccountController {

    private final EconomyPlugin plugin;

    private final Map<UUID, Account> accounts = new ConcurrentHashMap<>();
    private final Map<UUID, Account> onlineAccounts = new ConcurrentHashMap<>();
    private final List<Account> topAccounts = new ArrayList<>();
    private final Map<UUID, Long> soundCooldowns = new HashMap<>();

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
     * Retrieves an account from the global cache by its unique ID.
     *
     * @param id The UUID of the account.
     * @return An {@link Optional} containing the account if found.
     */
    public Optional<Account> getAccount(final UUID id) {
        return Optional.ofNullable(this.accounts.get(id));
    }

    /**
     * Retrieves an account from the global cache using a Bukkit player.
     *
     * @param player The online player.
     * @return An {@link Optional} containing the account if found.
     */
    public Optional<Account> getAccount(final Player player) {
        return this.getAccount(player.getUniqueId());
    }

    /**
     * Retrieves an account from the global cache by the account name.
     *
     * @param name The name associated with the account.
     * @return An {@link Optional} containing the account if found.
     */
    public Optional<Account> getAccount(final String name) {
        for (final Account account : this.accounts.values()) {
            if (account.getName().equals(name)) return Optional.of(account);
        }
        return Optional.empty();
    }

    /**
     * Retrieves an account from the online-only cache by its unique ID.
     *
     * @param id The UUID of the online player.
     * @return An {@link Optional} containing the account if the player is online.
     */
    public Optional<Account> getPlayerAccount(final UUID id) {
        return Optional.ofNullable(this.onlineAccounts.get(id));
    }

    /**
     * Retrieves an account from the online-only cache for a specific player.
     *
     * @param player The online player.
     * @return An {@link Optional} containing the account if the player is online.
     */
    public Optional<Account> getPlayerAccount(final Player player) {
        return this.getPlayerAccount(player.getUniqueId());
    }

    /**
     * Retrieves an account from the online-only cache by the player's name.
     *
     * @param name The name of the online player.
     * @return An {@link Optional} containing the account if the player is online.
     */
    public Optional<Account> getPlayerAccount(final String name) {
        for (final Account account : this.onlineAccounts.values()) {
            if (account.getName().equals(name)) return Optional.of(account);
        }
        return Optional.empty();
    }

    /**
     * Creates a new economy account with the configured starting balance
     * and adds it to the cache.
     *
     * @param id     The unique ID for the new account.
     * @param name   The name to associate with the account.
     * @param player {@code true} if the account is a player account.
     * @return The newly created {@link Account}.
     */
    public Account createAccount(final UUID id, final String name, final boolean player) {
        final BigDecimal balance = this.plugin.getConfigController().getCurrencyConfig().getStartingBalance();
        final Account account = Account.builder()
                .plugin(this.plugin)
                .id(id)
                .name(name)
                .balance(balance)
                .build();

        this.accounts.put(id, account);
        if (player) this.onlineAccounts.put(id, account);

        return account;
    }

    /**
     * Removes an account from the cache and deletes its data from the database.
     *
     * @param id The UUID of the account to delete.
     * @return {@code true} if the deletion process was initiated.
     */
    public boolean deleteAccount(final UUID id) {
        this.accounts.remove(id);

        this.plugin.getDataController().deleteAccount(id);
        return true;
    }

    /**
     * Loads all accounts from the database into the primary cache.
     *
     * @return A {@link CompletableFuture} that completes when the load is finished.
     */
    private CompletableFuture<Void> loadAccounts() {
        return this.plugin.getDataController().queryAccounts().thenAccept(accounts -> {
            for (final Account account : accounts) {
                this.accounts.put(account.getId(), account);
            }
        });
    }

    /**
     * Updates the name associated with an account in the cache.
     *
     * @param id   The UUID of the account.
     * @param name The new name to set.
     * @return {@code true} if the account was found and renamed successfully.
     */
    public boolean renameAccount(final UUID id, final String name) {
        final Account account = this.accounts.get(id);

        if (account != null) {
            return account.setName(name);
        }
        return false;
    }

    /**
     * Refreshes the leaderboard list with the provided set of accounts.
     *
     * @param accounts The new list of top accounts.
     */
    public void updateTopAccounts(final List<Account> accounts) {
        this.topAccounts.clear();
        this.topAccounts.addAll(accounts);
    }

    /**
     * Tracks an account in the online player cache.
     *
     * @param account The account belonging to the connecting player.
     */
    public void addPlayerAccount(final Account account) {
        this.onlineAccounts.put(account.getId(), account);
    }

    /**
     * Ceases tracking an account in the online player cache.
     *
     * @param account The account belonging to the disconnecting player.
     */
    public void removePlayerAccount(final Account account) {
        this.onlineAccounts.remove(account.getId());
    }

    /**
     * Synchronizes a player's Minecraft experience and level to match
     * their economy account balance.
     *
     * @param player The player whose experience bar should be updated.
     */
    public void updateExp(final Player player) {
        this.getPlayerAccount(player).ifPresent(account -> {
            final double balance = account.getBalance().doubleValue();
            final int newLevel = (int) balance;
            final float progress = (float) (balance - newLevel);

            if (newLevel / 5 > player.getLevel() / 5) {
                // Play level up sound every 5 levels
                final long currentTime = System.currentTimeMillis();
                final long lastPlayed = soundCooldowns.getOrDefault(player.getUniqueId(), 0L);

                if (currentTime - lastPlayed >= Constants.LEVEL_SOUND_COOLDOWN) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    soundCooldowns.put(player.getUniqueId(), currentTime);
                }
            }

            player.setLevel(newLevel);
            player.setExp(progress);
        });
    }

}
