package com.spektrsoyuz.economy.model.config;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

// Class for the currency config section
@Getter
@ConfigSerializable
public final class CurrencyConfig {

    private final String name;
    private final String namePlural;
    private final String nameSingular;
    private final double startingBalance;
    private final String symbol;

    // Constructor
    public CurrencyConfig() {
        this.name = "crowns";
        this.namePlural = "crowns";
        this.nameSingular = "crown";
        this.startingBalance = 0.0;
        this.symbol = "♛";
    }
}
