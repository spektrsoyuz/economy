package com.spektrsoyuz.economy.model;

import com.spektrsoyuz.economy.EconomyPlugin;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

// Model class for an economy account
@Getter
public final class Account {

    private final UUID id;
    private final Consumer<Account> accountConsumer;
    private final Consumer<Transaction> transactionConsumer;
    private BigDecimal balance;
    private String name;
    private boolean frozen;

    // Constructor
    @Builder
    public Account(
            final EconomyPlugin plugin,
            final UUID id,
            final String name,
            final BigDecimal balance,
            final Boolean frozen
    ) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.frozen = frozen != null && frozen;

        this.accountConsumer = plugin.getAccountQueueTask()::queue;
        this.transactionConsumer = plugin.getTransactionQueueTask()::queue;
    }

    // Gets the formatted name of the account
    public String getFormattedName() {
        // Tax account
        if (this.name.equals("tax")) {
            return "Server (tax)";
        }

        // Town account
        if (this.name.startsWith("town-")) {
            return this.name
                    .replaceFirst("town-", "")
                    .replace("_", " ");
        }

        // Nation account
        if (this.name.startsWith("nation-")) {
            return this.name
                    .replaceFirst("nation-", "")
                    .replace("_", " ");
        }

        return this.name;
    }

    // Sets the account name
    public boolean setName(final String name) {
        if (this.frozen) return false;
        this.name = name;

        this.saveAccount();
        return true;
    }

    // Adds to the account balance
    public boolean addBalance(final BigDecimal amount, final Transactor transactor) {
        if (this.frozen) return false;
        this.balance = this.balance.add(amount);

        this.saveAccount();
        this.saveTransaction(amount, transactor);
        return true;
    }

    // Sets the account balance
    public boolean setBalance(final BigDecimal balance, final Transactor transactor) {
        if (this.frozen) return false;
        final BigDecimal current = this.balance;
        this.balance = balance;

        this.saveAccount();
        this.saveTransaction(balance.subtract(current), transactor);
        return true;
    }

    // Subtracts from the account balance
    public boolean subtractBalance(final BigDecimal amount, final Transactor transactor) {
        if (this.frozen) return false;
        this.balance = this.balance.subtract(amount);

        this.saveAccount();
        this.saveTransaction(amount, transactor);
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
    private void saveTransaction(final BigDecimal amount, final Transactor transactor) {
        final Transaction transaction = new Transaction(this.id, this.name, amount, transactor);
        this.transactionConsumer.accept(transaction);
    }

    // Creates a Memento pattern
    public Memento createMemento() {
        return new Memento(
                this.id,
                this.name,
                this.balance,
                this.frozen
        );
    }

    // Memento pattern for an account
    public record Memento(
            UUID id,
            String name,
            BigDecimal balance,
            boolean frozen
    ) {
        // Creates an Account from a Memento pattern
        public Account toAccount(final EconomyPlugin plugin) {
            return new Account(
                    plugin,
                    id,
                    name,
                    balance,
                    frozen
            );
        }
    }
}
