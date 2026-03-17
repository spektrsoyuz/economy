package com.spektrsoyuz.economy;

import com.spektrsoyuz.economy.controller.ConfigController;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class EconomyPlugin extends JavaPlugin {

    private final ConfigController configController = new ConfigController(this);

    @Override
    public void onEnable() {
        // Plugin startup logic
        this.configController.initialize();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

    }

}
