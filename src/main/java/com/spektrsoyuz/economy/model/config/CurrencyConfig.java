package com.spektrsoyuz.economy.model.config;

import com.spektrsoyuz.economy.model.CurrencyType;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.math.BigDecimal;

/**
 * Model class for the currency config.
 *
 * @since 1.0.0
 */
@Getter
@ConfigSerializable
public final class CurrencyConfig {

    private final String name;
    private final String namePlural;
    private final String nameSingular;
    private final double startingBalance;
    private final String symbol;
    private final String type;

    // Constructor
    public CurrencyConfig() {
        this.name = "crowns";
        this.namePlural = "crowns";
        this.nameSingular = "crown";
        this.startingBalance = 0.0;
        this.symbol = "♛";
        this.type = "default";
    }

    // Gets the starting balance for new accounts
    public BigDecimal getStartingBalance() {
        return BigDecimal.valueOf(this.startingBalance);
    }

    // Gets the currency type
    public CurrencyType getType() {
        return CurrencyType.valueOf(this.type.toUpperCase());
    }

}
