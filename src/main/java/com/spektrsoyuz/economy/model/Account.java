package com.spektrsoyuz.economy.model;

import com.spektrsoyuz.economy.EconomyPlugin;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// Model class for an economy account
@Getter
public final class Account {

    private final UUID id;
    private final Consumer<Account> accountConsumer;
    private final Consumer<Transaction> transactionConsumer;
    private final Map<String, BigDecimal> balances;
    private String name;
    private boolean frozen;

    // Constructor
    public Account(
            final EconomyPlugin plugin,
            final UUID id,
            final String name,
            final Map<String, BigDecimal> balances,
            final boolean frozen
    ) {
        this.id = id;
        this.name = name;
        this.balances = balances;
        this.frozen = frozen;

        this.accountConsumer = plugin.getAccountQueueTask()::queue;
        this.transactionConsumer = plugin.getTransactionQueueTask()::queue;
    }

    // Sets the account name
    public boolean setName(final String name) {
        if (this.frozen) return false;
        this.name = name;

        this.saveAccount();
        return true;
    }

    // Gets the account balance
    public BigDecimal getBalance(final String currency) {
        return this.balances.getOrDefault(currency, BigDecimal.ZERO);
    }

    // Adds to the account balance
    public boolean addBalance(final String currency, final BigDecimal amount, final Transactor transactor) {
        if (this.frozen) return false;

        final BigDecimal current = this.getBalance(currency);
        this.balances.put(currency, current.add(amount));

        this.saveAccount();
        this.saveTransaction(currency, amount, transactor);
        return true;
    }

    // Sets the account balance
    public boolean setBalance(final String currency, final BigDecimal balance, final Transactor transactor) {
        if (this.frozen) return false;

        final BigDecimal current = this.getBalance(currency);
        this.balances.put(currency, balance);

        this.saveAccount();
        this.saveTransaction(currency, balance.subtract(current), transactor);
        return true;
    }

    // Subtracts from the account balance
    public boolean subtractBalance(final String currency, final BigDecimal amount, final Transactor transactor) {
        if (this.frozen) return false;

        final BigDecimal current = getBalance(currency);
        this.balances.put(currency, current.subtract(amount));

        this.saveAccount();
        this.saveTransaction(currency, amount, transactor);
        return true;
    }

    // Sets the account frozen state
    public void setFrozen(final boolean frozen) {
        this.frozen = frozen;

        this.saveAccount();
    }

    // Freezes the account
    public void freeze() {
        this.frozen = true;

        this.saveAccount();
    }

    // Unfreezes the account
    public void unfreeze() {
        this.frozen = false;

        this.saveAccount();
    }

    // Saves the account state
    private void saveAccount() {
        this.accountConsumer.accept(this);
    }

    // Saves a transaction
    private void saveTransaction(final String currency, final BigDecimal amount, final Transactor transactor) {
        final Transaction transaction = new Transaction(this.id, this.name, currency, amount, transactor);
        this.transactionConsumer.accept(transaction);
    }

    // Creates a Memento pattern
    public Memento createMemento() {
        return new Memento(
                this.id,
                this.name,
                this.balances,
                this.frozen
        );
    }

    // Memento pattern for an account
    public record Memento(
            UUID id,
            String name,
            Map<String, BigDecimal> balances,
            boolean frozen
    ) {
        // Creates an Account from a Memento pattern
        public Account toAccount(final EconomyPlugin plugin) {
            return new Account(
                    plugin,
                    id,
                    name,
                    balances,
                    frozen
            );
        }
    }
}
