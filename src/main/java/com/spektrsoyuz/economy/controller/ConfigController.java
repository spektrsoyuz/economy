package com.spektrsoyuz.economy.controller;

import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.StorageConfig;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Controller class for plugin configuration
@RequiredArgsConstructor
public final class ConfigController {

    private final EconomyPlugin plugin;

    private CommentedConfigurationNode primaryConfig;
    private CommentedConfigurationNode messagesConfig;

    /**
     * Initializes the controller.
     */
    public void initialize() {
        this.primaryConfig = this.createNode("config.yml");
        this.messagesConfig = this.createNode("messages.yml");
    }

    /**
     * Creates a YAML configuration loader for a specified file path.
     *
     * @param path The path to the configuration file
     * @return A configured {@link YamlConfigurationLoader}
     */
    private YamlConfigurationLoader createLoader(final Path path) {
        return YamlConfigurationLoader.builder()
                .path(path)
                .indent(2)
                .build();
    }

    /**
     * Loads a configuration file into a {@link CommentedConfigurationNode}.
     *
     * @param file The name of the configuration file
     * @return The loaded {@link CommentedConfigurationNode}, or {@code null} if loading fails
     */
    private CommentedConfigurationNode createNode(final String file) {
        final Path path = this.plugin.getDataPath().resolve(file);

        if (!Files.exists(path)) {
            // Create file if it does not exist
            this.plugin.saveResource(file, false);
        }
        final var loader = this.createLoader(path);

        try {
            return loader.load();
        } catch (final ConfigurateException e) {
            this.plugin.getComponentLogger().error("Failed to load config file '{}'", file, e);
            return null;
        }
    }

    /**
     * Gets the version of the loaded primary configuration.
     *
     * @return The configuration version number
     */
    public int getConfigVersion() {
        return this.primaryConfig.node("config-version").getInt(0);
    }

    /**
     * Gets the storage configuration section from the primary configuration.
     *
     * @return The {@link StorageConfig} object
     */
    public @NotNull StorageConfig getStorageConfig() {
        try {
            final StorageConfig storageConfig = this.primaryConfig.node("storage").get(StorageConfig.class);
            return storageConfig != null ? storageConfig : new StorageConfig();
        } catch (final SerializationException e) {
            this.plugin.getComponentLogger().error("Failed to load storage config", e);
            return new StorageConfig();
        }
    }

    /**
     * Gets the message prefix from the message config
     *
     * @return The message prefix
     */
    public @NotNull String getPrefix() {
        return this.primaryConfig.node("prefix").getString("prefix");
    }

    /**
     * Gets a message from the message config.
     *
     * @param key       Message key
     * @param resolvers List of TagResolver objects
     * @return The message as a TextComponent object
     */
    public @NotNull Component getMessage(final String key, final TagResolver... resolvers) {
        final String message = this.messagesConfig.node(key).getString();

        final List<TagResolver> tagResolvers = new ArrayList<>(List.of(resolvers));
        tagResolvers.add(Placeholder.parsed("prefix", getPrefix()));

        return message != null
                ? MiniMessage.miniMessage().deserialize(message, tagResolvers.toArray(TagResolver[]::new))
                : Component.text(key);
    }
}
