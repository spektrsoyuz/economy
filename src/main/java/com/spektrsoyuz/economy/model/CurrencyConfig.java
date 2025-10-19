package com.spektrsoyuz.economy.model;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public final class CurrencyConfig {

    private final String name;
    private final String nameSingular;
    private final String namePlural;
    private final String symbol;

    // Constructor
    public CurrencyConfig() {
        this.name = "crowns";
        this.nameSingular = "crown";
        this.namePlural = "crowns";
        this.symbol = "";
    }
}
