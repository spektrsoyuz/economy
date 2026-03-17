package com.spektrsoyuz.economy.controller;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.account.Account;
import com.spektrsoyuz.economy.model.account.Transaction;
import com.spektrsoyuz.economy.model.config.OptionsConfig;
import com.spektrsoyuz.economy.model.config.StorageConfig;
import com.spektrsoyuz.economy.model.data.EconomyArgumentFactory;
import com.spektrsoyuz.economy.model.data.EconomyDao;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Controller class for data.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class DataController {

    private final EconomyPlugin plugin;

    private HikariDataSource dataSource;
    private Jdbi jdbi;

    public void initialize() {
        final StorageConfig storageConfig = this.plugin.getConfigController().getStorageConfig();
        final OptionsConfig optionsConfig = this.plugin.getConfigController().getOptionsConfig();

        final HikariConfig hikariConfig = this.getHikariConfig(storageConfig);

        try {
            this.dataSource = new HikariDataSource(hikariConfig);
            this.jdbi = Jdbi.create(this.dataSource);
            this.jdbi.installPlugin(new SqlObjectPlugin());
        } catch (final Exception e) {
            this.plugin.getComponentLogger().error("Error initializing database connection", e);
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            return;
        }

        this.jdbi.registerArgument(new EconomyArgumentFactory());

        this.jdbi.useExtension(EconomyDao.class, dao -> {
            dao.createAccountsTable();
            dao.createTransactionsTable();

            if (optionsConfig.isAccountExpire()) {
                dao.expireAccounts(optionsConfig.getAccountExpireDuration());
            }

            if (optionsConfig.isTransactionExpire()) {
                dao.expireTransactions(optionsConfig.getTransactionExpireDuration());
            }
        });
    }

    private @NotNull HikariConfig getHikariConfig(final StorageConfig storageConfig) {
        final HikariConfig hikariConfig = new HikariConfig();
        final String url = String.format("jdbc:mysql://%s:%s/%s",
                storageConfig.getHost(),
                storageConfig.getPort(),
                storageConfig.getDatabase()
        );

        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(storageConfig.getUsername());
        hikariConfig.setPassword(storageConfig.getPassword());

        // Performance optimizations for MySQL
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

        return hikariConfig;
    }

    public void saveAccount(final Account.Memento account) {
        CompletableFuture.runAsync(() ->
                this.jdbi.useExtension(EconomyDao.class, dao -> dao.saveAccount(account))
        ).exceptionally(ex -> {
            this.plugin.getComponentLogger().error("Error saving accountId '{}'", account.id(), ex);
            return null;
        });
    }

    public void saveTransaction(final Transaction transaction) {
        CompletableFuture.runAsync(() ->
                this.jdbi.useExtension(EconomyDao.class, dao -> dao.saveTransaction(transaction))
        ).exceptionally(ex -> {
            this.plugin.getComponentLogger().error("Error saving transaction", ex);
            return null;
        });
    }

    public void deleteAccount(final UUID id) {
        CompletableFuture.runAsync(() ->
                this.jdbi.useExtension(EconomyDao.class, dao -> dao.deleteAccount(id.toString()))
        ).exceptionally(ex -> {
            this.plugin.getComponentLogger().error("Error deleting accountId '{}'", id, ex);
            return null;
        });
    }

    public CompletableFuture<Optional<Account>> queryAccount(final UUID id) {
        return CompletableFuture.supplyAsync(() ->
                this.jdbi.withExtension(EconomyDao.class, dao ->
                        dao.getAccountById(id.toString())
                                .map(memento -> memento.toAccount(this.plugin)))
        ).exceptionally(ex -> {
            this.plugin.getComponentLogger().error("Error querying accountId by UUID: {}", id, ex);
            return Optional.empty();
        });
    }

    public CompletableFuture<Optional<Account>> queryAccount(final String name) {
        return CompletableFuture.supplyAsync(() ->
                this.jdbi.withExtension(EconomyDao.class, dao ->
                        dao.getAccountByName(name)
                                .map(memento -> memento.toAccount(this.plugin)))
        ).exceptionally(ex -> {
            this.plugin.getComponentLogger().error("Error querying accountId by accountName: {}", name, ex);
            return Optional.empty();
        });
    }

    public CompletableFuture<Set<Account>> queryAccounts() {
        return CompletableFuture.supplyAsync(() ->
                this.jdbi.withExtension(EconomyDao.class, dao ->
                        dao.getAccounts().stream()
                                .map(memento -> memento.toAccount(this.plugin))
                                .collect(Collectors.toSet()))
        ).exceptionally(ex -> {
            this.plugin.getComponentLogger().error("Error querying all accounts", ex);
            return Set.of();
        });
    }

    public void close() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }
}