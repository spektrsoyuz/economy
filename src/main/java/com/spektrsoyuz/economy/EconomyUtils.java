package com.spektrsoyuz.economy;

import java.math.BigDecimal;

public final class EconomyUtils {

    // Constants
    public static final String PLUGIN_NAME = "ae-economy";

    // Command permissions
    public static final String PERMISSION_COMMAND_BALANCE = "economy.balance";
    public static final String PERMISSION_COMMAND_BALANCE_OTHER = "economy.balance.other";
    public static final String PERMISSION_COMMAND_ECONOMY = "economy.admin";
    public static final String PERMISSION_COMMAND_PAY = "economy.pay";

    public static String format(final EconomyPlugin plugin, BigDecimal amount) {
        return amount.compareTo(BigDecimal.ONE) != 0
                ? plugin.getConfigController().getCurrencyConfig().getNamePlural()
                : plugin.getConfigController().getCurrencyConfig().getNameSingular();
    }
}
