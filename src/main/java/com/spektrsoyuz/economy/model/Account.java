package com.spektrsoyuz.economy.model;

import com.spektrsoyuz.economy.EconomyPlugin;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

@Getter
public final class Account {

    private final UUID id;
    private final Consumer<Account> accountConsumer;
    private final Consumer<Transaction> transactionConsumer;
    private BigDecimal balance;
    private String name;
    private boolean frozen;

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

        this.accountConsumer = null; // TODO plugin.getAccountQueueTask()::queue;
        this.transactionConsumer = null; // TODO plugin.getTransactionQueueTask()::queue;

        this.saveAccount();
    }

    public String getDisplayName() {
        if (this.name.equals("tax")) {
            return "Server (tax)";
        }

        if (this.name.startsWith("town-")) {
            return this.name
                    .replaceFirst("town-", "")
                    .replace("_", " ");
        }

        if (this.name.startsWith("nation-")) {
            return this.name
                    .replaceFirst("nation-", "")
                    .replace("_", " ");
        }

        return this.name;
    }

    public boolean setName(final String name) {
        if (this.frozen) return false;
        this.name = name;

        this.saveAccount();
        return true;
    }

    public boolean setBalance(final BigDecimal balance, final Transactor transactor) {
        if (this.frozen) return false;
        final BigDecimal current = this.balance;
        this.balance = balance;

        this.saveAccount();
        this.saveTransaction(balance.subtract(current), transactor);
        return true;
    }

    public boolean addBalance(final BigDecimal amount, final Transactor transactor) {
        if (this.frozen) return false;
        this.balance = this.balance.add(amount);

        this.saveAccount();
        this.saveTransaction(amount, transactor);
        return true;
    }

    public boolean subtractBalance(final BigDecimal amount, final Transactor transactor) {
        if (this.frozen) return false;
        this.balance = this.balance.subtract(amount);

        this.saveAccount();
        this.saveTransaction(amount.negate(), transactor);
        return true;
    }

    public void setFrozen(final boolean frozen) {
        this.frozen = frozen;

        this.saveAccount();
    }

    public void freeze() {
        this.frozen = true;

        this.saveAccount();
    }

    public void unfreeze() {
        this.frozen = false;

        this.saveAccount();
    }

    private void saveAccount() {
        this.accountConsumer.accept(this);
    }

    private void saveTransaction(final BigDecimal amount, final Transactor transactor) {
        final Transaction transaction = new Transaction(this.id, this.name, amount, transactor);
        this.transactionConsumer.accept(transaction);
    }

    public Memento createMemento() {
        return new Memento(
                this.id,
                this.name,
                this.balance,
                this.frozen
        );
    }

    public record Memento(
            UUID id,
            String name,
            BigDecimal balance,
            boolean frozen
    ) {
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
