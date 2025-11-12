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

        this.plugin.getComponentLogger().debug("Added account '{}:{}' to the queue",
                account.getId(),
                account.getName()
        );
    }

    @Override
    public void run() {

    }
}
