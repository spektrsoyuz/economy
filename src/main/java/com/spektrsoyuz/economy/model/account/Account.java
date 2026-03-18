package com.spektrsoyuz.economy.model.account;

import com.spektrsoyuz.economy.EconomyPlugin;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Model class for an economy account.
 *
 * @since 1.0.0
 */
@Getter
public final class Account {

    private final UUID id;
    private final Consumer<Account> accountConsumer;
    private final Consumer<Transaction> transactionConsumer;
    private BigDecimal balance;
    private String name;
    private boolean frozen;
    private boolean autoDeposit;

    /**
     * Constructs a new Account and initializes the update consumers.
     *
     * @param plugin      The economy plugin instance.
     * @param id          The unique identifier for the account.
     * @param name        The internal name of the account.
     * @param balance     The starting balance.
     * @param frozen      The frozen status (defaults to false if null).
     * @param autoDeposit The auto deposit status (defaults to false if null).
     */
    @Builder
    public Account(
            final EconomyPlugin plugin,
            final UUID id,
            final String name,
            final BigDecimal balance,
            final Boolean frozen,
            final Boolean autoDeposit
    ) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.frozen = frozen != null && frozen;
        this.autoDeposit = autoDeposit != null && autoDeposit;

        this.accountConsumer = plugin.getAccountQueueTask()::queue;
        this.transactionConsumer = plugin.getTransactionQueueTask()::queue;

        this.saveAccount();
    }

    /**
     * Translates the internal name into a user-friendly display string.
     *
     * @return A formatted display name.
     */
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

    /**
     * Updates the internal name of the account.
     *
     * @param name The new name to set.
     * @return {@code true} if the name was updated.
     */
    public boolean setName(final String name) {
        if (this.frozen) return false;
        this.name = name;

        this.saveAccount();
        return true;
    }

    /**
     * Sets the account balance to a specific value and logs the transaction.
     *
     * @param balance    The new balance.
     * @param transactor The actor responsible for the change.
     * @return {@code true} if the balance was updated.
     */
    public boolean setBalance(final BigDecimal balance, final Transactor transactor) {
        if (this.frozen) return false;
        final BigDecimal current = this.balance;
        this.balance = balance;

        this.saveAccount();
        this.saveTransaction(balance.subtract(current), transactor);
        return true;
    }

    /**
     * Increments the account balance by a specified amount.
     *
     * @param amount     The amount to add.
     * @param transactor The actor responsible for the change.
     * @return {@code true} if the balance was updated.
     */
    public boolean addBalance(final BigDecimal amount, final Transactor transactor) {
        if (this.frozen) return false;
        this.balance = this.balance.add(amount);

        this.saveAccount();
        this.saveTransaction(amount, transactor);
        return true;
    }

    /**
     * Decrements the account balance by a specified amount.
     *
     * @param amount     The amount to subtract.
     * @param transactor The actor responsible for the change.
     * @return {@code true} if the balance was updated.
     */
    public boolean subtractBalance(final BigDecimal amount, final Transactor transactor) {
        if (this.frozen) return false;
        this.balance = this.balance.subtract(amount);

        this.saveAccount();
        this.saveTransaction(amount.negate(), transactor);
        return true;
    }

    /**
     * Sets the frozen status of the account, preventing further modifications.
     *
     * @param frozen The new frozen state.
     */
    public void setFrozen(final boolean frozen) {
        this.frozen = frozen;

        this.saveAccount();
    }

    /**
     * Locks the account to prevent balance or name changes.
     */
    public void freeze() {
        this.frozen = true;

        this.saveAccount();
    }

    /**
     * Unlocks the account to allow modifications.
     */
    public void unfreeze() {
        this.frozen = false;

        this.saveAccount();
    }

    /**
     * Sets the auto deposit status of the account
     *
     * @param autoDeposit The new auto deposit state.
     */
    public void setAutoDeposit(final boolean autoDeposit) {
        this.autoDeposit = autoDeposit;

        this.saveAccount();
    }

    /**
     * Queues the current account state for persistence.
     */
    private void saveAccount() {
        this.accountConsumer.accept(this);
    }

    /**
     * Creates and queues a transaction record for persistence.
     *
     * @param amount     The change in balance (positive or negative).
     * @param transactor The actor responsible for the change.
     */
    private void saveTransaction(final BigDecimal amount, final Transactor transactor) {
        final Transaction transaction = new Transaction(this.id, this.name, amount, transactor);
        this.transactionConsumer.accept(transaction);
    }

    /**
     * Creates an immutable snapshot of the current account state.
     *
     * @return A new {@link Memento} containing current account data.
     */
    public Memento createMemento() {
        return new Memento(
                this.id,
                this.name,
                this.balance,
                this.frozen,
                this.autoDeposit
        );
    }

    /**
     * An immutable snapshot of an account's state for data transfer
     * and persistence.
     */
    public record Memento(
            UUID id,
            String name,
            BigDecimal balance,
            boolean frozen,
            boolean autoDeposit
    ) {
        public Account toAccount(final EconomyPlugin plugin) {
            return new Account(
                    plugin,
                    id,
                    name,
                    balance,
                    frozen,
                    autoDeposit
            );
        }
    }

}
