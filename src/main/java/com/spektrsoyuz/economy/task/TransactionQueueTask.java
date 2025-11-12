package com.spektrsoyuz.economy.task;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.Transaction;
import lombok.RequiredArgsConstructor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

// Task for saving transactions
@RequiredArgsConstructor
public final class TransactionQueueTask extends BukkitRunnable {

    private final EconomyPlugin plugin;
    private final List<Transaction> queue = new ArrayList<>();

    // Adds a transaction to the queue
    public void queue(final Transaction transaction) {
        this.queue.add(transaction);

        this.plugin.getComponentLogger().debug("Added transaction for account '{}:{}' to the queue",
                transaction.accountId(),
                transaction.accountName()
        );
    }

    @Override
    public void run() {

    }
}
