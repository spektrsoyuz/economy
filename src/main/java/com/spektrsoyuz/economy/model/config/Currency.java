package com.spektrsoyuz.economy.model.config;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

// Class for the currency config section
@Getter
@ConfigSerializable
public final class Currency {

    private final String name;
    @Setting("name-plural")
    private final String namePlural;
    @Setting("name-singular")
    private final String nameSingular;
    @Setting("starting-balance")
    private final double startingBalance;
    private final String symbol;

    // Constructor
    public Currency() {
        this.name = "crowns";
        this.namePlural = "crowns";
        this.nameSingular = "crown";
        this.startingBalance = 0.0;
        this.symbol = "♛";
    }
}
