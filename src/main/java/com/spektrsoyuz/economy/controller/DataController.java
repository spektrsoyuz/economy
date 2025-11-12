package com.spektrsoyuz.economy.controller;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.Account;
import com.spektrsoyuz.economy.model.Transaction;
import com.spektrsoyuz.economy.model.config.OptionsConfig;
import com.spektrsoyuz.economy.model.config.StorageConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

// Controller class for database interaction
@RequiredArgsConstructor
public final class DataController {

    private final EconomyPlugin plugin;

    private HikariDataSource dataSource;

    // SQL statements
    private String sqlSaveAccount;
    private String sqlSaveTransaction;
    private String sqlDeleteAccount;
    private String sqlExpireAccounts;
    private String sqlExpireTransactions;
    private String sqlQueryAccountId;
    private String sqlQueryAccountName;
    private String sqlQueryAccounts;
    private String sqlCreateAccountsTable;
    private String sqlCreateTransactionsTable;

    // Initializes the controller
    public void initialize() {
        final StorageConfig storageConfig = this.plugin.getConfigController().getStorageConfig();
        final OptionsConfig optionsConfig = this.plugin.getConfigController().getOptionsConfig();
        final HikariConfig hikariConfig = this.getHikariConfig(storageConfig);
        final String type = storageConfig.getType();

        // Attempt to create the database connection
        try {
            this.dataSource = new HikariDataSource(hikariConfig);
        } catch (final HikariPool.PoolInitializationException e) {
            // Failed to connect, throw exception and disable plugin
            this.plugin.getComponentLogger().error("Error initializing database connection", e);
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            return;
        }

        if (type.equalsIgnoreCase("mysql")) {
            // MySQL/MariaDB database
            this.sqlSaveAccount = "INSERT INTO economy_accounts (id, name, balance, frozen) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE name = ?, balance = ?, frozen = ?;";
            this.sqlCreateTransactionsTable = """
                    CREATE TABLE IF NOT EXISTS economy_transactions (
                        id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        account VARCHAR(36) NOT NULL,
                        actor TEXT NOT NULL,
                        name TEXT NOT NULL,
                        amount DECIMAL(10,2) NOT NULL
                    );
                    """;
            this.sqlExpireAccounts = "DELETE FROM economy_accounts WHERE timestamp < DATE_SUB(NOW(), INTERVAL ? DAY);";
            this.sqlExpireTransactions = "DELETE FROM economy_transactions WHERE timestamp < DATE_SUB(NOW(), INTERVAL ? DAY);";
        } else {
            // SQLite database
            this.sqlSaveAccount = "INSERT INTO economy_accounts (id, name, balance, frozen) VALUES (?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET name = VALUES(name), balance = VALUES(balance), frozen = VALUES(frozen);";
            this.sqlCreateTransactionsTable = """
                    CREATE TABLE IF NOT EXISTS economy_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        account TEXT NOT NULL,
                        actor TEXT NOT NULL,
                        name TEXT NOT NULL,
                        amount REAL NOT NULL
                    );
                    """;
            this.sqlExpireAccounts = "DELETE FROM economy_accounts WHERE timestamp < DATETIME('now', ?);";
            this.sqlExpireTransactions = "DELETE FROM economy_transactions WHERE timestamp < DATETIME('now', ?);";
        }

        this.sqlSaveTransaction = "INSERT INTO economy_transactions (account, actor, name, amount) VALUES (?, ?, ?, ?);";
        this.sqlDeleteAccount = "DELETE FROM economy_accounts WHERE id = ?;";
        this.sqlQueryAccountId = "SELECT * FROM economy_accounts WHERE id = ?;";
        this.sqlQueryAccountName = "SELECT * FROM economy_accounts WHERE name = ?;";
        this.sqlQueryAccounts = "SELECT * FROM economy_accounts;";
        this.sqlCreateAccountsTable = """
                CREATE TABLE IF NOT EXISTS economy_accounts (
                    id VARCHAR(36) NOT NULL UNIQUE PRIMARY KEY,
                    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    name TEXT NOT NULL,
                    balance DECIMAL(10,2) NOT NULL,
                    frozen BOOLEAN NOT NULL
                );
                """;

        this.createAccountsTable();
        this.createTransactionsTable();

        // Handle account expiration
        if (optionsConfig.isAccountExpire()) {
            final int duration = optionsConfig.getAccountExpireDuration();
            this.expireAccounts(type, duration);
        }

        // Handle transaction expiration
        if (optionsConfig.isTransactionExpire()) {
            final int duration = optionsConfig.getTransactionExpireDuration();
            this.expireTransactions(type, duration);
        }
    }

    // Asynchronously saves an account to the database
    private @NotNull HikariConfig getHikariConfig(final StorageConfig storageConfig) {
        final HikariConfig hikariConfig = new HikariConfig();

        if (storageConfig.getType().equalsIgnoreCase("mysql")) {
            // MySQL/MariaDB database
            final String url = String.format("jdbc:mysql://%s:%s/%s",
                    storageConfig.getHost(),
                    storageConfig.getPort(),
                    storageConfig.getDatabase()
            );

            hikariConfig.setJdbcUrl(url);
            hikariConfig.setUsername(storageConfig.getUsername());
            hikariConfig.setPassword(storageConfig.getPassword());
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            // SQLite database
            final String url = "jdbc:sqlite:data.db";

            hikariConfig.setJdbcUrl(url);
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
        }

        return hikariConfig;
    }

    // Asynchronously saves an account to the database
    public void saveAccount(final Account account) {
        CompletableFuture.runAsync(() -> {
            try {
                this.executeUpdate(this.sqlSaveAccount,
                        account.getId().toString(), account.getName(), account.getBalance(), account.isFrozen(),
                        account.getName(), account.getBalance(), account.isFrozen()
                );
            } catch (final SQLException e) {
                this.plugin.getComponentLogger().error("Error saving account '{}'", account.getId(), e);
            }
        });
    }

    // Asynchronously saves a transaction to the database
    public void saveTransaction(final Transaction transaction) {
        CompletableFuture.runAsync(() -> {
            try {
                this.executeUpdate(this.sqlSaveTransaction,
                        transaction.accountId(),
                        transaction.transactor().name(),
                        transaction.accountName(),
                        transaction.amount()
                );
            } catch (final SQLException e) {
                this.plugin.getComponentLogger().error("Error saving transaction for account '{}'",
                        transaction.accountId(), e);
            }
        });
    }

    // Asynchronously deletes an account from the database
    public void deleteAccount(final UUID id) {
        CompletableFuture.runAsync(() -> {
            try {
                this.executeUpdate(this.sqlDeleteAccount,
                        id.toString());
            } catch (final SQLException e) {
                this.plugin.getComponentLogger().error("Error deleting account '{}'", id, e);
            }
        });
    }

    // Asynchronously deletes expired accounts
    public void expireAccounts(final String type, final int days) {
        CompletableFuture.runAsync(() -> {
            try {
                final Object parameter = type.equalsIgnoreCase("mysql")
                        ? days
                        : "-" + days + " days";

                this.executeUpdate(this.sqlExpireAccounts, parameter);
            } catch (final SQLException e) {
                this.plugin.getComponentLogger().error("Error deleting old accounts", e);
            }
        });
    }

    // Asynchronously deletes expired transactions
    public void expireTransactions(final String type, final int days) {
        CompletableFuture.runAsync(() -> {
            try {
                final Object parameter = type.equalsIgnoreCase("mysql")
                        ? days
                        : "-" + days + " days";

                this.executeUpdate(this.sqlExpireTransactions, parameter);
            } catch (final SQLException e) {
                this.plugin.getComponentLogger().error("Error deleting old transactions", e);
            }
        });
    }

    // Asynchronously queries an account from the database by its unique ID
    public CompletableFuture<Account> queryAccount(final UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.executeQuery(this.sqlQueryAccountId,
                        resultSet -> resultSet.next() ? this.parseAccount(resultSet) : null,
                        id.toString());
            } catch (final SQLException e) {
                this.plugin.getComponentLogger().error("Error getting account '{}'", id, e);
                return null;
            }
        });
    }

    // Asynchronously queries an account from the database by its name
    public CompletableFuture<Account> queryAccount(final String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.executeQuery(this.sqlQueryAccountName,
                        resultSet -> resultSet.next() ? this.parseAccount(resultSet) : null,
                        name);
            } catch (final SQLException e) {
                this.plugin.getComponentLogger().error("Error getting account '{}'", name, e);
                return null;
            }
        });
    }

    // Asynchronously queries all accounts from the database
    public CompletableFuture<Set<Account>> queryAccounts() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.executeQuery(this.sqlQueryAccounts,
                        this::parseAccounts);
            } catch (final SQLException e) {
                this.plugin.getComponentLogger().error("Error getting accounts", e);
                return null;
            }
        });
    }

    // Parses a ResultSet into a set of Account objects
    private Set<Account> parseAccounts(final ResultSet resultSet) throws SQLException {
        final Set<Account> accounts = new HashSet<>();
        while (resultSet.next()) {
            accounts.add(this.parseAccount(resultSet));
        }
        return accounts;
    }

    // Parses the current row of a ResultSet into an Account object
    private Account parseAccount(final ResultSet resultSet) throws SQLException {
        final UUID id = UUID.fromString(resultSet.getString("id"));
        final String name = resultSet.getString("name");
        final BigDecimal balance = resultSet.getBigDecimal("balance");
        final boolean frozen = resultSet.getBoolean("frozen");

        // Create a new account
        return new Account(this.plugin, id, name, balance, frozen);
    }

    // Creates the 'accounts' table in the database if it does not already exist
    private void createAccountsTable() {
        try {
            this.executeUpdate(this.sqlCreateAccountsTable);
        } catch (final SQLException e) {
            this.plugin.getComponentLogger().error("Error creating accounts table", e);
        }
    }

    // Creates the 'transactions' table in the database if it does not already exist
    private void createTransactionsTable() {
        try {
            this.executeUpdate(this.sqlCreateTransactionsTable);
        } catch (final SQLException e) {
            this.plugin.getComponentLogger().error("Error creating transactions table", e);
        }
    }

    // Closes the database connection
    public void close() {
        try {
            this.dataSource.close();
        } catch (final RuntimeException e) {
            this.plugin.getComponentLogger().error("Error closing data connection", e);
        }
    }

    // Executes a database query and maps the ResultSet to an object
    private <T> @Nullable T executeQuery(final String query, final ThrowingFunction<ResultSet, T> mapper, final @Nullable Object... parameters) throws SQLException {
        try (final Connection connection = this.dataSource.getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                if (parameters != null) {
                    for (int i = 0; i < parameters.length; i++) {
                        preparedStatement.setObject(i + 1, parameters[i]);
                    }
                }
                try (final ResultSet resultSet = preparedStatement.executeQuery()) {
                    return ThrowingFunction.unchecked(mapper).apply(resultSet);
                }
            }
        }
    }

    // Executes a data manipulation statement (e.g., INSERT, UPDATE, DELETE)
    private void executeUpdate(final String query, final @Nullable Object... parameters) throws SQLException {
        try (final Connection connection = this.dataSource.getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                if (parameters != null) {
                    for (int i = 0; i < parameters.length; i++) {
                        preparedStatement.setObject(i + 1, parameters[i]);
                    }
                }
                preparedStatement.executeUpdate();
            }
        }
    }

    // A functional interface for a function that can throw an SQLException
    @FunctionalInterface
    protected interface ThrowingFunction<T, R> {
        static <T, R> ThrowingFunction<T, R> unchecked(final ThrowingFunction<T, R> f) {
            return f;
        }

        @Nullable
        R apply(T t) throws SQLException;
    }
}
