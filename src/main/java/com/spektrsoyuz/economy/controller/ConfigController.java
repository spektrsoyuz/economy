package com.spektrsoyuz.economy.controller;

import com.spektrsoyuz.economy.Constants;
import com.spektrsoyuz.economy.EconomyPlugin;
import com.spektrsoyuz.economy.model.config.CurrencyConfig;
import com.spektrsoyuz.economy.model.config.OptionsConfig;
import com.spektrsoyuz.economy.model.config.StorageConfig;
import io.github.miniplaceholders.api.MiniPlaceholders;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Controller class for plugin configuration
@RequiredArgsConstructor
public final class ConfigController {

    private final EconomyPlugin plugin;

    private CommentedConfigurationNode primaryConfig;
    private CommentedConfigurationNode messagesConfig;

    // Initializes the controller
    public boolean initialize() {
        this.primaryConfig = this.createNode(Constants.CONFIG_PRIMARY);
        this.messagesConfig = this.createNode(Constants.CONFIG_MESSAGES);

        return this.primaryConfig != null && this.messagesConfig != null;
    }

    // Creates a HOCON configuration loader for a specified file path
    private HoconConfigurationLoader createLoader(final Path path) {
        return HoconConfigurationLoader.builder()
                .path(path)
                .indent(2)
                .build();
    }

    // Loads a configuration file into a config node
    private CommentedConfigurationNode createNode(final String file) {
        final Path path = this.plugin.getDataPath().resolve(file);

        if (Files.notExists(path)) {
            // Create file if not exists
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

    // Resets a configuration file
    public void reset(final String file) {
        final Path path = this.plugin.getDataPath().resolve(file);

        if (Files.exists(path)) {
            // Delete file if exists
            try {
                Files.delete(path);
            } catch (final IOException e) {
                this.plugin.getComponentLogger().error("Failed to delete config file '{}'", file, e);
            }
        }
    }

    // Gets the message prefix from the primary config
    public @NotNull String getPrefix() {
        return this.primaryConfig.node("command-prefix").getString("");
    }

    // Gets the options config from the primary config
    public @NotNull OptionsConfig getOptionsConfig() {
        try {
            final var config = this.primaryConfig.node("options").get(OptionsConfig.class);
            return config != null ? config : new OptionsConfig();
        } catch (final ConfigurateException e) {
            this.plugin.getComponentLogger().error("Failed to get options config", e);
            return new OptionsConfig();
        }
    }

    // Gets the storage config from the primary config
    public @NotNull CurrencyConfig getCurrencyConfig() {
        try {
            final var config = this.primaryConfig.node("currency").get(CurrencyConfig.class);
            return config != null ? config : new CurrencyConfig();
        } catch (final ConfigurateException e) {
            this.plugin.getComponentLogger().error("Failed to get currency config", e);
            return new CurrencyConfig();
        }
    }

    // Gets the storage config from the primary config
    public @NotNull StorageConfig getStorageConfig() {
        try {
            final var config = this.primaryConfig.node("storage").get(StorageConfig.class);
            return config != null ? config : new StorageConfig();
        } catch (final ConfigurateException e) {
            this.plugin.getComponentLogger().error("Failed to get storage config", e);
            return new StorageConfig();
        }
    }

    // Gets a message from the message config
    public @NotNull Component getMessage(final String key, final TagResolver... resolvers) {
        return this.getMessage(key, null, resolvers);
    }

    // Helper method to get a message from the message config
    public @NotNull Component getMessage(final String key, @Nullable final Player player, final TagResolver... resolvers) {
        String message = this.messagesConfig.node(key).getString();

        if (message == null) {
            return Component.text(key);
        }

        final List<TagResolver> tagResolvers = new ArrayList<>(List.of(resolvers));
        tagResolvers.add(Placeholder.parsed("prefix", getPrefix()));

        // Parse placeholders from MiniPlaceholders
        if (this.plugin.getServer().getPluginManager().isPluginEnabled("MiniPlaceholders")) {
            tagResolvers.add(MiniPlaceholders.globalPlaceholders());
        }

        // Parse placeholders from PlaceholderAPI
        if (player != null && this.plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        // Deserialize the message
        return MiniMessage.miniMessage().deserialize(message, tagResolvers.toArray(TagResolver[]::new));
    }
}
