package com.spektrsoyuz.economy.task;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.Account;
import lombok.RequiredArgsConstructor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Task for saving accounts
@RequiredArgsConstructor
public final class AccountQueueTask extends BukkitRunnable {

    private final EconomyPlugin plugin;
    private final List<UUID> queue = new ArrayList<>();

    // Adds an account to the queue
    public void queue(final Account account) {
        this.queue.add(account.getId());

        if (this.plugin.getConfigController().getOptionsConfig().isDebug()) {
            this.plugin.getComponentLogger().debug("Added account '{}:{}' to the queue",
                    account.getId(),
                    account.getName()
            );
        }
    }

    @Override
    public void run() {
        final List<Account.Memento> mementos = new ArrayList<>();

        final List<UUID> uuids = this.plugin.getAccountController().getAccounts()
                .values()
                .stream()
                .map(Account::getId)
                .toList();

        // Create snapshot of all queued accounts
        for (final UUID uuid : uuids) {
            if (this.queue.contains(uuid)) {
                this.plugin.getAccountController().getAccount(uuid).ifPresent(account -> mementos.add(account.createMemento()));
            }
        }

        // Clear the queue
        this.queue.clear();

        // Save accounts to database
        for (final Account.Memento memento : mementos) {
            this.plugin.getDataController().saveAccount(memento.toAccount(this.plugin));

            if (this.plugin.getConfigController().getOptionsConfig().isDebug()) {
                this.plugin.getComponentLogger().warn("Saved account '{}:{}' to the database", memento.id(), memento.name());
            }
        }
    }
}
