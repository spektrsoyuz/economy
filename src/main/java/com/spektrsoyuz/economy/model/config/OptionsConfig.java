package com.spektrsoyuz.economy.model.config;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

// Class for the options config section
@Getter
@ConfigSerializable
public final class OptionsConfig {

    @Setting("default-currency")
    private final String defaultCurrency;
    private final boolean debug;
    @Setting("account-expire")
    private final boolean accountExpire;
    @Setting("account-expire-duration")
    private final int accountExpireDuration;
    @Setting("transaction-expire")
    private final boolean transactionExpire;
    @Setting("transaction-expire-duration")
    private final int transactionExpireDuration;

    // Constructor
    public OptionsConfig() {
        this.defaultCurrency = "crowns";
        this.debug = false;
        this.accountExpire = false;
        this.accountExpireDuration = 90;
        this.transactionExpire = false;
        this.transactionExpireDuration = 90;
    }
}
