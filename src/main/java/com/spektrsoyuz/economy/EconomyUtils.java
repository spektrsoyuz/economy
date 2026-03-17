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
        final CurrencyConfig currencyConfig = plugin.getConfigController().getCurrencyConfig();

        final String name = amount.compareTo(BigDecimal.ONE) != 0
                ? currencyConfig.getNamePlural()
                : currencyConfig.getNameSingular();

        // Experience formatting
        if (currencyConfig.getType().equals("exp")) {
            final long totalPoints = amount.longValue();
            final int levelCost = Constants.LEVEL_COST;

            final long levels = totalPoints / levelCost;
            final long remainder = totalPoints % levelCost;

            final int precision = String.valueOf(levelCost - 1).length();

            return String.format(
                    "%s%d.%0" + precision + "d %s",
                    currencyConfig.getSymbol(),
                    levels,
                    remainder,
                    name
            );
        }

        // Default formatting
        return String.format("%s%s %s",
                currencyConfig.getSymbol(),
                amount,
                name
        );
    }

}
