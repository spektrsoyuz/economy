package com.spektrsoyuz.economy.model;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public final class CurrencyConfig {

    private final String name = "crowns";
    private final String nameSingular = "crown";
    private final String namePlural = "crowns";
    private final String symbol = "";
}
