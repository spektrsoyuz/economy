package com.spektrsoyuz.economy;

import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Class for plugin-wide utility methods.
 *
 * @since 1.0.0
 */
public final class EconomyUtils {

    // Returns a formatted currency string
    public static String format(final EconomyPlugin plugin, BigDecimal amount) {
        final CurrencyConfig config = plugin.getConfigController().getCurrencyConfig();

        final DecimalFormat df = new DecimalFormat("#,##0.##");
        df.setRoundingMode(RoundingMode.HALF_UP);

        final String formattedAmount = df.format(amount);

        final String label = (amount.compareTo(BigDecimal.ONE) == 0)
                ? config.getNameSingular()
                : config.getNamePlural();

        return String.format("%s%s %s", config.getSymbol(), formattedAmount, label);
    }

    // Plays an error sound for a player
    public static void playErrorSound(final Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1.0f, 0.5f);
    }

}
