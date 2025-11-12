package com.spektrsoyuz.economy;

import com.spektrsoyuz.economy.command.BalanceCommand;
import com.spektrsoyuz.economy.command.BalanceTopCommand;
import com.spektrsoyuz.economy.command.EconomyCommand;
import com.spektrsoyuz.economy.command.PayCommand;
import com.spektrsoyuz.economy.controller.AccountController;
import com.spektrsoyuz.economy.controller.ConfigController;
import com.spektrsoyuz.economy.controller.DataController;
import com.spektrsoyuz.economy.listener.PlayerListener;
import com.spektrsoyuz.economy.model.vault.EconomyImpl;
import com.spektrsoyuz.economy.model.vault.LegacyEconomyImpl;
import com.spektrsoyuz.economy.task.AccountQueueTask;
import com.spektrsoyuz.economy.task.AccountSyncTask;
import com.spektrsoyuz.economy.task.TransactionQueueTask;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.milkbowl.vault2.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;

// Main class for the economy plugin
@Getter
public final class EconomyPlugin extends JavaPlugin {

    private final AccountController accountController = new AccountController(this);
    private final ConfigController configController = new ConfigController(this);
    private final DataController dataController = new DataController(this);

    private final AccountQueueTask accountQueueTask = new AccountQueueTask(this);
    private final TransactionQueueTask transactionQueueTask = new TransactionQueueTask(this);
    private final AccountSyncTask accountSyncTask = new AccountSyncTask(this);

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

        this.dataController.initialize();
        this.accountController.initialize();

        // Check for Vault
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            this.getComponentLogger().error("Failed to get provider for Vault plugin, disabling plugin");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.registerCommands();
        this.registerListeners();
        this.registerTasks();

        // Register economy
        new EconomyImpl(this).register();
        new LegacyEconomyImpl(this).register();

        // Verify economy registration
        if (this.getServer().getServicesManager().load(Economy.class) == null) {
            this.getComponentLogger().error("Failed to register Economy with Vault, disabling plugin");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.dataController.close();
    }

    private void registerCommands() {
        // Register commands
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();

            new BalanceCommand(this).register(registrar);
            new BalanceTopCommand(this).register(registrar);
            new EconomyCommand(this).register(registrar);
            new PayCommand(this).register(registrar);
        });
    }

    private void registerListeners() {
        // Register listeners
        new PlayerListener(this);
    }

    private void registerTasks() {
        // Register tasks
        // ticks = seconds * 20
        this.accountQueueTask.runTaskTimerAsynchronously(this, 0, 30 * 20); // run every 30 seconds
        this.transactionQueueTask.runTaskTimerAsynchronously(this, 0, 30 * 20); // run every 30 seconds
        this.accountSyncTask.runTaskTimer(this, 0, 30 * 20); // run every 30 seconds
    }
}
