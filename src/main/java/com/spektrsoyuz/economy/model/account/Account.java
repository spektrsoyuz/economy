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

    // Constructor
    public Account(
            final EconomyPlugin plugin,
            final UUID id,
            final String name,
            final BigDecimal balance
    ) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.consumer = plugin.getDataController()::saveAccount;

        this.save();
    }

    // Sets the name of the account
    public boolean setName(final String name) {
        this.name = name;

        this.save();
        return true;
    }

    // Adds to the account balance
    public boolean addBalance(final BigDecimal amount) {
        this.balance = this.balance.add(amount);

        this.save();
        return true;
    }

    // Sets the account balance
    public boolean setBalance(final BigDecimal balance) {
        this.balance = balance;

        this.save();
        return true;
    }

    // Subtracts from the account balance
    public boolean subtractBalance(final BigDecimal amount) {
        this.balance = this.balance.subtract(amount);

        this.save();
        return true;
    }

    // Saves the account state
    private void save() {
        this.consumer.accept(this);
    }
}
