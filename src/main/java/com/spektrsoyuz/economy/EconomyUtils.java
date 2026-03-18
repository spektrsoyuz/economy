package com.spektrsoyuz.economy;

import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // Distributes physical items to the player based on value
    public static void distributeItems(final Player player, final CurrencyConfig config, final int totalValue) {
        int remainingToGive = totalValue;

        // Get currency items sorted by value descending
        final List<Map.Entry<String, Integer>> sortedItems = new ArrayList<>(config.getItems().entrySet());
        sortedItems.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        for (final Map.Entry<String, Integer> entry : sortedItems) {
            if (remainingToGive <= 0) break;

            final Material material = Material.matchMaterial(entry.getKey());
            if (material == null) continue;

            final int itemValue = entry.getValue();
            final int countToGive = remainingToGive / itemValue;

            if (countToGive > 0) {
                final ItemStack stack = new ItemStack(material, countToGive);

                // Give items to player
                player.getInventory().addItem(stack).values().forEach(leftover -> {
                    // Drop remaining items
                    player.getWorld().dropItem(player.getLocation(), leftover);
                });

                remainingToGive %= itemValue;
            }
        }
    }

}
