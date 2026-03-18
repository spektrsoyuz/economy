package com.spektrsoyuz.economy.model.config;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.math.BigDecimal;

/**
 * Model class for the options config.
 *
 * @since 1.0.0
 */
@Getter
@ConfigSerializable
public final class OptionsConfig {

    private final boolean debug;
    private final boolean accountExpire;
    private final int accountExpireDuration;
    private final boolean transactionExpire;
    private final int transactionExpireDuration;
    private final boolean loseBalanceOnDeath;
    private final int loseBalanceOnDeathAmount;

    // Constructor
    public OptionsConfig() {
        this.debug = false;
        this.accountExpire = false;
        this.accountExpireDuration = 90;
        this.transactionExpire = false;
        this.transactionExpireDuration = 90;
        this.loseBalanceOnDeath = false;
        this.loseBalanceOnDeathAmount = 5;
    }

    // Gets the amount lost on player death
    public BigDecimal getLoseBalanceOnDeathAmount() {
        return BigDecimal.valueOf(this.loseBalanceOnDeathAmount);
    }

}
