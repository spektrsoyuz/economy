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

    @SqlUpdate("""
            CREATE TABLE IF NOT EXISTS economy_accounts (
                id VARCHAR(36) NOT NULL UNIQUE PRIMARY KEY,
                timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                name TEXT NOT NULL,
                balance DECIMAL(10,2) NOT NULL,
                frozen BOOLEAN NOT NULL
            )""")
    void createAccountsTable();

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

    @SqlUpdate("""
            INSERT INTO economy_accounts (id, name, balance, frozen)
            VALUES (:id, :name, :balance, :frozen)
            ON DUPLICATE KEY UPDATE name = :name, balance = :balance, frozen = :frozen
            """)
    void saveAccount(@BindMethods Account.Memento account);

    @SqlUpdate("""
            INSERT INTO economy_transactions (account, actor, name, amount)
            VALUES (:accountId, :transactor, :accountName, :amount)
            """)
    void saveTransaction(@BindMethods Transaction transaction);

    @SqlUpdate("DELETE FROM economy_accounts WHERE id = :id")
    void deleteAccount(@Bind("id") String id);

    @SqlUpdate("DELETE FROM economy_accounts WHERE timestamp < DATE_SUB(NOW(), INTERVAL :days DAY)")
    void expireAccounts(@Bind("days") int days);

    @SqlUpdate("DELETE FROM economy_transactions WHERE timestamp < DATE_SUB(NOW(), INTERVAL :days DAY)")
    void expireTransactions(@Bind("days") int days);

    @SqlQuery("SELECT id, name, balance, frozen FROM economy_accounts WHERE id = :id")
    @RegisterConstructorMapper(Account.Memento.class)
    Optional<Account.Memento> getAccountById(@Bind("id") String id);

    @SqlQuery("SELECT id, name, balance, frozen FROM economy_accounts WHERE name = :name")
    @RegisterConstructorMapper(Account.Memento.class)
    Optional<Account.Memento> getAccountByName(@Bind("name") String name);

    @SqlQuery("SELECT id, name, balance, frozen FROM economy_accounts")
    @RegisterConstructorMapper(Account.Memento.class)
    Set<Account.Memento> getAccounts();

}