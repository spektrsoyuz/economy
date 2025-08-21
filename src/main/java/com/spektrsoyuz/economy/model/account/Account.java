package com.spektrsoyuz.economy.model.account;

import com.spektrsoyuz.economy.EconomyPlugin;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;

// Model class for economy accounts
@Getter
public final class Account {

    private final UUID id;
    private final Consumer<Account> consumer;
    private String name;
    private BigDecimal balance;

    /**
     * Constructor for an Account.
     *
     * @param plugin  Economy plugin instance
     * @param id      Account uuid
     * @param name    Account name
     * @param balance Starting balance
     */
    public Account(
            final EconomyPlugin plugin,
            final UUID id,
            final String name,
            final BigDecimal balance
    ) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.consumer = null; //plugin.getDataController().saveAccount();

        this.save();
    }

    /**
     * Sets the name of the account.
     *
     * @param name New name
     * @return true if succeeded, false otherwise
     */
    public boolean setName(final String name) {
        this.name = name;

        this.save();
        return true;
    }

    /**
     * Adds to the account balance.
     *
     * @param amount Amount to add to the account
     * @return true if successful, false otherwise
     */
    public boolean addBalance(final BigDecimal amount) {
        this.balance = this.balance.add(amount);

        this.save();
        return true;
    }

    /**
     * Saves the account state.
     */
    private void save() {
        this.consumer.accept(this);
    }
}
