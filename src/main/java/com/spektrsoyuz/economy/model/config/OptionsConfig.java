package com.spektrsoyuz.economy.model.config;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

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

    // Constructor
    public OptionsConfig() {
        this.debug = false;
        this.accountExpire = false;
        this.accountExpireDuration = 90;
        this.transactionExpire = false;
        this.transactionExpireDuration = 90;
    }
}
