package com.spektrsoyuz.economy.controller;

import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.config.ConfigWrapper;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import com.spektrsoyuz.economy.model.config.OptionsConfig;
import com.spektrsoyuz.economy.model.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller class for config files.
 *
 * @since 1.0.0
 */
@RequiredArgsConstructor
public final class ConfigController {

    private final EconomyPlugin plugin;

    private final Map<String, ConfigWrapper> configs = new ConcurrentHashMap<>();

    // Initializes the controller
    public void initialize() {
        this.configs.put(Constants.CONFIG_PRIMARY, new ConfigWrapper(this.plugin, Constants.CONFIG_PRIMARY));
        this.configs.put(Constants.CONFIG_MESSAGES, new ConfigWrapper(this.plugin, Constants.CONFIG_MESSAGES));

        this.load();
    }

    // Loads the configs
    public void load() {
        for (final ConfigWrapper config : this.configs.values()) {
            config.load();
        }
    }

    // Saves the configs
    public void save() {
        for (final ConfigWrapper config : this.configs.values()) {
            config.save();
        }
    }

    // Returns the loaded state
    public boolean isLoaded() {
        for (final ConfigWrapper config : this.configs.values()) {
            if (config == null) return false;
        }
        return true;
    }

    // Returns the currency config
    public @NotNull CurrencyConfig getCurrencyConfig() {
        return this.getConfig("currency", CurrencyConfig.class, new CurrencyConfig());
    }

    // Returns the options config
    public @NotNull OptionsConfig getOptionsConfig() {
        return this.getConfig("options", OptionsConfig.class, new OptionsConfig());
    }

    // Returns the storage config
    public @NotNull StorageConfig getStorageConfig() {
        return this.getConfig("storage", StorageConfig.class, new StorageConfig());
    }

    // Gets a config
    private @NotNull <T> T getConfig(
            final String nodeName,
            final Class<T> clazz,
            final T defaultInstance
    ) {
        try {
            final ConfigWrapper configWrapper = this.configs.get(Constants.CONFIG_PRIMARY);
            final T config = configWrapper.getNode().node(nodeName).get(clazz);

            return config != null
                    ? config
                    : defaultInstance;
        } catch (final SerializationException e) {
            this.plugin.getComponentLogger().error("Failed to load {} config, using default values", nodeName, e);
            return defaultInstance;
        }
    }

    // Returns the command prefix
    public @NotNull String getPrefix() {
        final ConfigWrapper configWrapper = this.configs.get(Constants.CONFIG_MESSAGES);
        final String prefix = configWrapper.getNode().node("prefix").getString();

        return prefix != null
                ? prefix
                : "";
    }

    // Returns a localization message
    public @NotNull Component getMessage(
            final String key,
            final MiniMessage miniMessage,
            final TagResolver... resolvers
    ) {
        final ConfigWrapper configWrapper = this.configs.get(Constants.CONFIG_MESSAGES);
        final String message = configWrapper.getNode().node(key).getString();

        final List<TagResolver> tagResolvers = new ArrayList<>(List.of(resolvers));
        tagResolvers.add(Placeholder.parsed("prefix", this.getPrefix()));

        if (message != null) {
            // Deserialize message
            return miniMessage.deserialize(message, tagResolvers.toArray(TagResolver[]::new))
                    .decorationIfAbsent(
                            TextDecoration.ITALIC,
                            TextDecoration.State.FALSE
                    );
        }

        return Component.text(key);
    }

}
