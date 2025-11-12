package com.spektrsoyuz.economy;

import com.spektrsoyuz.economy.model.config.CurrencyConfig;

import java.math.BigDecimal;

public final class EconomyUtils {

    // Creates a formatted currency string
    public static String format(final EconomyPlugin plugin, BigDecimal amount) {
        final CurrencyConfig currencyConfig = plugin.getConfigController().getCurrencyConfig();

        return String.format("%s%s %s",
                currencyConfig.getSymbol(),
                amount, amount.compareTo(BigDecimal.ONE) != 0
                        ? currencyConfig.getNamePlural()
                        : currencyConfig.getNameSingular());
    }

}
