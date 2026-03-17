package com.spektrsoyuz.economy;

import com.spektrsoyuz.economy.model.config.CurrencyConfig;

import java.math.BigDecimal;

/**
 * Class for plugin-wide utility methods.
 *
 * @since 1.0.0
 */
public final class EconomyUtils {

    // Returns a formatted currency string
    public static String format(final EconomyPlugin plugin, BigDecimal amount) {
        final CurrencyConfig config = plugin.getConfigController().getCurrencyConfig();

        final String label = (amount.compareTo(BigDecimal.ONE) == 0)
                ? config.getNameSingular()
                : config.getNamePlural();

        // Handle experience formatting
        if ("exp".equalsIgnoreCase(config.getType())) {
            final long totalPoints = amount.longValue();
            final int levelCost = Constants.LEVEL_COST;

            final long levels = totalPoints / levelCost;
            final long displayDecimal = (totalPoints % levelCost * 100) / levelCost;

            return String.format("%s%d.%02d %s", config.getSymbol(), levels, displayDecimal, label);
        }

        // Handle default formatting
        return String.format("%s%s %s", config.getSymbol(), amount.toPlainString(), label);
    }

}
