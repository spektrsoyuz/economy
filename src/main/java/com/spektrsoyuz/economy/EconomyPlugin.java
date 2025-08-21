package com.spektrsoyuz.economy;

import com.spektrsoyuz.economy.controller.AccountController;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyPlugin extends JavaPlugin {

    private final AccountController accountController = new AccountController(this);

    @Override
    public void onLoad() {
        // Plugin load logic
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.accountController.initialize();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
