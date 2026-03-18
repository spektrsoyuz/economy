package com.spektrsoyuz.economy.model.data;

import com.spektrsoyuz.economy.model.account.Account;
import com.spektrsoyuz.economy.model.account.Transaction;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;
import java.util.Set;

/**
 * Data access object for the economy.
 *
 * @since 1.0.0
 */
public interface EconomyDao {

    /**
     * Creates the primary accounts table if it does not already exist.
     */
    @SqlUpdate("""
            CREATE TABLE IF NOT EXISTS economy_accounts (
                id VARCHAR(36) NOT NULL UNIQUE PRIMARY KEY,
                timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                name TEXT NOT NULL,
                balance DECIMAL(10,2) NOT NULL,
                frozen BOOLEAN NOT NULL,
                auto_deposit BOOLEAN NOT NULL
            )""")
    void createAccountsTable();

    /**
     * Creates the transactions log table if it does not already exist.
     */
    @SqlUpdate("""
            CREATE TABLE IF NOT EXISTS economy_transactions (
                id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                account VARCHAR(36) NOT NULL,
                actor TEXT NOT NULL,
                name TEXT NOT NULL,
                amount DECIMAL(10,2) NOT NULL
            )""")
    void createTransactionsTable();

    /**
     * Inserts or updates an account record in the database.
     *
     * @param account The account state to persist.
     */
    @SqlUpdate("""
            INSERT INTO economy_accounts (id, name, balance, frozen, auto_deposit)
            VALUES (:id, :name, :balance, :frozen, :autoDeposit)
            ON DUPLICATE KEY UPDATE name = :name, balance = :balance, frozen = :frozen, auto_deposit = :autoDeposit
            """)
    void saveAccount(@BindMethods Account.Memento account);

    /**
     * Records a new transaction entry in the database log.
     *
     * @param transaction The transaction details to persist.
     */
    @SqlUpdate("""
            INSERT INTO economy_transactions (account, actor, name, amount)
            VALUES (:accountId, :transactor, :accountName, :amount)
            """)
    void saveTransaction(@BindMethods Transaction transaction);

    /**
     * Deletes a specific account from the database by its unique ID.
     *
     * @param id The UUID string of the account to remove.
     */
    @SqlUpdate("DELETE FROM economy_accounts WHERE id = :id")
    void deleteAccount(@Bind("id") String id);

    /**
     * Deletes account records that have not been updated within
     * the specified timeframe.
     *
     * @param days The age threshold in days for expiration.
     */
    @SqlUpdate("DELETE FROM economy_accounts WHERE timestamp < DATE_SUB(NOW(), INTERVAL :days DAY)")
    void expireAccounts(@Bind("days") int days);

    /**
     * Deletes transaction logs older than the specified timeframe.
     *
     * @param days The age threshold in days for expiration.
     */
    @SqlUpdate("DELETE FROM economy_transactions WHERE timestamp < DATE_SUB(NOW(), INTERVAL :days DAY)")
    void expireTransactions(@Bind("days") int days);

    /**
     * Retrieves an account state from the database by its unique ID.
     *
     * @param id The UUID string to search for.
     * @return An {@link Optional} containing the account memento if found.
     */
    @SqlQuery("SELECT id, name, balance, frozen, auto_deposit FROM economy_accounts WHERE id = :id")
    @RegisterConstructorMapper(Account.Memento.class)
    Optional<Account.Memento> getAccountById(@Bind("id") String id);

    /**
     * Retrieves an account state from the database by the owner's name.
     *
     * @param name The name to search for.
     * @return An {@link Optional} containing the account memento if found.
     */
    @SqlQuery("SELECT id, name, balance, frozen, auto_deposit FROM economy_accounts WHERE name = :name")
    @RegisterConstructorMapper(Account.Memento.class)
    Optional<Account.Memento> getAccountByName(@Bind("name") String name);

    /**
     * Retrieves all account states currently stored in the database.
     *
     * @return A set containing all account mementos.
     */
    @SqlQuery("SELECT id, name, balance, frozen, auto_deposit FROM economy_accounts")
    @RegisterConstructorMapper(Account.Memento.class)
    Set<Account.Memento> getAccounts();

}