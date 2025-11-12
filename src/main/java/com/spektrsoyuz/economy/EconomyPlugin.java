package com.spektrsoyuz.economy;

import com.spektrsoyuz.economy.controller.ConfigController;
import com.spektrsoyuz.economy.task.AccountQueueTask;
import com.spektrsoyuz.economy.task.TransactionQueueTask;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

// Main class for the economy plugin
@Getter
public final class EconomyPlugin extends JavaPlugin {

    private final ConfigController configController = new ConfigController(this);

    private final AccountQueueTask accountQueueTask = new AccountQueueTask(this);
    private final TransactionQueueTask transactionQueueTask = new TransactionQueueTask(this);

    @Override
    public void onLoad() {
        // Plugin load logic
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        final boolean success = this.configController.initialize();

        // Check if config loaded
        if (!(success)) {
            this.getComponentLogger().error("Failed to load config files");
        }

        // Check for Vault
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            this.getComponentLogger().error("Failed to get provider for Vault plugin, disabling plugin");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.registerTasks();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerCommands() {
        // Register commands
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();


        });
    }

    private void registerListeners() {
        // Register listeners

    }

    private void registerTasks() {
        // Register tasks
        // ticks = seconds * 20
        this.accountQueueTask.runTaskTimerAsynchronously(this, 0, 30 * 20); // run every 30 seconds
        this.transactionQueueTask.runTaskTimerAsynchronously(this, 0, 30 * 20); // run every 30 seconds
    }
}
