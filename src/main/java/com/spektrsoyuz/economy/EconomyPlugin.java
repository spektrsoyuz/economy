package com.spektrsoyuz.economy;

import com.spektrsoyuz.economy.task.AccountQueueTask;
import com.spektrsoyuz.economy.task.TransactionQueueTask;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

// Main class for the economy plugin
@Getter
public final class EconomyPlugin extends JavaPlugin {

    private final AccountQueueTask accountQueueTask = new AccountQueueTask(this);
    private final TransactionQueueTask transactionQueueTask = new TransactionQueueTask(this);

    @Override
    public void onLoad() {
        // Plugin load logic
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.registerTasks();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerTasks() {
        // Register tasks
        // ticks = seconds * 20
        this.accountQueueTask.runTaskTimerAsynchronously(this, 0, 30 * 20); // run every 30 seconds
        this.transactionQueueTask.runTaskTimerAsynchronously(this, 0, 30 * 20); // run every 30 seconds
    }
}
