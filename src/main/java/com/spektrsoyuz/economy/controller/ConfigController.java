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

    /**
     * Initializes the controller.
     */
    public void initialize() {
        this.configs.put(Constants.CONFIG_PRIMARY, new ConfigWrapper(this.plugin, Constants.CONFIG_PRIMARY));
        this.configs.put(Constants.CONFIG_MESSAGES, new ConfigWrapper(this.plugin, Constants.CONFIG_MESSAGES));

        this.load();
    }

    /**
     * Loads all configuration files.
     */
    public void load() {
        for (final ConfigWrapper config : this.configs.values()) {
            config.load();
        }
    }

    /**
     * Saves all configuration files.
     */
    public void save() {
        for (final ConfigWrapper config : this.configs.values()) {
            config.save();
        }
    }

    /**
     * Checks if all required configurations have been successfully initialized.
     *
     * @return {@code true} if all configurations are loaded.
     */
    public boolean isLoaded() {
        for (final ConfigWrapper config : this.configs.values()) {
            if (config == null) return false;
        }
        return true;
    }

    /**
     * Retrieves the currency-specific settings from the primary configuration.
     *
     * @return A {@link CurrencyConfig} instance, or defaults if loading fails.
     */
    public @NotNull CurrencyConfig getCurrencyConfig() {
        return this.getConfig("currency", CurrencyConfig.class, new CurrencyConfig());
    }

    /**
     * Retrieves the general plugin options from the primary configuration.
     *
     * @return An {@link OptionsConfig} instance, or defaults if loading fails.
     */
    public @NotNull OptionsConfig getOptionsConfig() {
        return this.getConfig("options", OptionsConfig.class, new OptionsConfig());
    }

    /**
     * Retrieves the database storage settings from the primary configuration.
     *
     * @return A {@link StorageConfig} instance, or defaults if loading fails.
     */
    public @NotNull StorageConfig getStorageConfig() {
        return this.getConfig("storage", StorageConfig.class, new StorageConfig());
    }

    /**
     * Helper method to deserialize a specific node from the primary configuration
     * into a given class type.
     *
     * @param nodeName        The name of the configuration node.
     * @param clazz           The class to map the data to.
     * @param defaultInstance The instance to return if mapping fails.
     * @return The mapped configuration object.
     */
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

    /**
     * Retrieves the global message prefix from the messages configuration.
     *
     * @return The configured prefix string, or an empty string if not found.
     */
    public @NotNull String getPrefix() {
        final ConfigWrapper configWrapper = this.configs.get(Constants.CONFIG_MESSAGES);
        final String prefix = configWrapper.getNode().node("prefix").getString();

        return prefix != null
                ? prefix
                : "";
    }

    /**
     * Retrieves and processes a localized message from the messages configuration.
     *
     * @param key         The localization key in the config.
     * @param miniMessage The MiniMessage instance to use for deserialization.
     * @param resolvers   Additional tag resolvers for dynamic placeholders.
     * @return A formatted {@link Component} ready for display.
     */
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
