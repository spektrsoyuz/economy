package com.spektrsoyuz.economy.model.config;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

// Class for the options config section
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
