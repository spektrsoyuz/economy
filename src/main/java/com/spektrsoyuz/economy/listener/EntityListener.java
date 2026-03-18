package com.spektrsoyuz.economy.listener;

import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ExpBottleEvent;

/**
 * Listener class for entity events.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class EntityListener implements Listener {

    private final EconomyPlugin plugin;

    // Registers the listener
    public void register() {
        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler
    public void onExpBottleBreak(final ExpBottleEvent event) {
        // Set experience drop to fixed value
        event.setExperience(Constants.LEVEL_COST);
    }

}
