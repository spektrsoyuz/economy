package com.spektrsoyuz.economy.model.config;

import com.spektrsoyuz.economy.model.CurrencyType;
import lombok.Getter;
import org.bukkit.Material;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.math.BigDecimal;
import java.util.Map;

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
    private final Map<String, Integer> items;
    private final ExpConfig exp;

    // Constructor
    public CurrencyConfig() {
        this.name = "crowns";
        this.namePlural = "crowns";
        this.nameSingular = "crown";
        this.startingBalance = 0.0;
        this.symbol = "";
        this.type = "default";
        this.items = Map.of("minecraft:gold_ingot", 1, "minecraft:gold_block", 9);
        this.exp = new ExpConfig();
    }

    @Getter
    @ConfigSerializable
    public static class ExpConfig {
        private final int cost;
        private final int cooldown;

        // Constructor
        public ExpConfig() {
            this.cost = 64;
            this.cooldown = 5000;
        }
    }

    // Gets the starting balance for new accounts
    public BigDecimal getStartingBalance() {
        return BigDecimal.valueOf(this.startingBalance);
    }

    // Gets the currency type
    public CurrencyType getType() {
        return CurrencyType.valueOf(this.type.toUpperCase());
    }

    /**
     * Gets the value of a specific Material.
     *
     * @return the value, or 0 if not a valid item
     */
    public int getItemValue(final Material material) {
        if (material == null) return 0;

        final String key = material.getKey().getKey().toLowerCase();
        return items.getOrDefault(key, 0);
    }

}
