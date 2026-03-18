package com.spektrsoyuz.economy.task;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.CurrencyType;
import com.spektrsoyuz.economy.model.account.Account;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asynchronous task for saving accounts.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class AccountQueueTask implements Runnable {

    private final EconomyPlugin plugin;
    private final Set<UUID> queue = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Adds an account to the queue
    public void queue(final Account account) {
        this.queue.add(account.getId());

        // Sync XP bar
        final Player player = this.plugin.getServer().getPlayer(account.getId());
        final CurrencyConfig currencyConfig = this.plugin.getConfigController().getCurrencyConfig();

        if (player != null && currencyConfig.getType() == CurrencyType.EXP) {
            // Sync XP bar
            this.plugin.getAccountController().updateExp(player);
        }

        if (this.plugin.getConfigController().getOptionsConfig().isDebug()) {
            this.plugin.getComponentLogger().debug("Added account '{}:{}' to the queue",
                    account.getId(),
                    account.getName()
            );
        }
    }

    @Override
    public void run() {
        if (this.queue.isEmpty()) return;

        // Empty the queue
        UUID[] toProcess;
        synchronized (this.queue) {
            toProcess = this.queue.toArray(new UUID[0]);
            this.queue.clear();
        }

        // Save accounts
        for (final UUID uuid : toProcess) {
            this.plugin.getAccountController().getAccount(uuid).ifPresent(account -> {
                try {
                    final Account.Memento memento = account.createMemento();
                    this.plugin.getDataController().saveAccount(memento);

                    if (this.plugin.getConfigController().getOptionsConfig().isDebug()) {
                        this.plugin.getComponentLogger().warn("Saved account '{}:{}' to the database",
                                memento.id(), memento.name());
                    }
                } catch (final Exception e) {
                    this.plugin.getComponentLogger().error("Failed to save account '{}'", uuid, e);
                    this.queue.add(uuid);
                }
            });
        }
    }

}
