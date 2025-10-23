package com.spektrsoyuz.economy.controller;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.StorageConfig;
import com.spektrsoyuz.economy.model.account.Account;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// Controller class for database interaction
@RequiredArgsConstructor
public final class DataController {

    private final EconomyPlugin plugin;

    private HikariDataSource dataSource;

    // Initializes the controller
    public void initialize() {
        final HikariConfig hikariConfig = this.getHikariConfig();

        // Attempt to create the database connection
        try {
            this.dataSource = new HikariDataSource(hikariConfig);
        } catch (final HikariPool.PoolInitializationException e) {
            // Failed to connect, throw exception and disable plugin
            this.plugin.getComponentLogger().error("Error initializing database connection", e);
            this.plugin.getServer().getPluginManager().disablePlugin(this.plugin);
            return;
        }

        this.createAccountsTable();
    }

    // Asynchronously saves an account to the database
    private @NotNull HikariConfig getHikariConfig() {
        final HikariConfig hikariConfig = new HikariConfig();
        final StorageConfig storageConfig = this.plugin.getConfigController().getStorageConfig();

        final String jdbcUrl = "jdbc:mysql://" + storageConfig.getHost() + ":" + storageConfig.getPort() + "/" + storageConfig.getDatabase();
        final String username = storageConfig.getUsername();
        final String password = storageConfig.getPassword();

        if (storageConfig.getType().equalsIgnoreCase("mysql")) {
            // MySQL/MariaDB database
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            // SQLite database
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
        }

        hikariConfig.setMaximumPoolSize(10);

        return hikariConfig;
    }

    // Asynchronously saves an account to the database
    public void saveAccount(final Account account) {
        CompletableFuture.runAsync(() -> {
            try {
                this.executeUpdate("INSERT INTO accounts (id, name, balance) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name = ?, balance = ?;",
                        account.getId().toString(), account.getName(), account.getBalance(),
                        account.getName(), account.getBalance()
                );
            } catch (final SQLException e) {
                this.plugin.getComponentLogger().error("Error saving account '{}'", account.getId(), e);
            }
        });
    }

    // Asynchronously deletes an account from the database
    public void deleteAccount(final UUID id) {
        CompletableFuture.runAsync(() -> {
            try {
                this.executeUpdate("DELETE FROM accounts WHERE id = ?;",
                        id.toString());
            } catch (final SQLException e) {
                this.plugin.getComponentLogger().error("Error deleting account '{}'", id, e);
            }
        });
    }

    // Asynchronously queries an account from the database by its unique ID
    public CompletableFuture<Account> queryAccount(final UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return this.executeQuery("SELECT * FROM accounts WHERE id = ?;",
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
                return this.executeQuery("SELECT * FROM accounts WHERE name = ?;",
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
                return this.executeQuery("SELECT * FROM accounts;",
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

        // Create a new account
        return new Account(this.plugin, id, name, balance);
    }

    // Creates the 'accounts' table in the database if it does not already exist
    private void createAccountsTable() {
        try {
            this.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id VARCHAR(36) NOT NULL UNIQUE PRIMARY KEY,
                        name TEXT NOT NULL,
                        balance DECIMAL(10,2) NOT NULL
                    );
                    """);
        } catch (final SQLException e) {
            this.plugin.getComponentLogger().error("Error creating accounts table", e);
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
