package com.spektrsoyuz.economy;

import com.spektrsoyuz.economy.command.BalanceCommand;
import com.spektrsoyuz.economy.controller.AccountController;
import com.spektrsoyuz.economy.controller.ConfigController;
import com.spektrsoyuz.economy.controller.DataController;
import com.spektrsoyuz.economy.listener.PlayerListener;
import com.spektrsoyuz.economy.model.vault.EconomyImpl;
import com.spektrsoyuz.economy.model.vault.LegacyEconomyImpl;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import net.milkbowl.vault2.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;

// Main class for the plugin
@Getter
public final class EconomyPlugin extends JavaPlugin {

    private final AccountController accountController = new AccountController(this);
    private final ConfigController configController = new ConfigController(this);
    private final DataController dataController = new DataController(this);

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.configController.initialize();
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
        if (this.dataController != null) {
            this.dataController.close();
        }
    }

    private void registerCommands() {
        // Register Paper commands
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();

            new BalanceCommand(this).register(registrar);
        });
    }

    private void registerListeners() {
        // Register listener classes
        this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
    }
}
