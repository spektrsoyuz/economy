package com.spektrsoyuz.economy.model.config;

import com.spektrsoyuz.economy.EconomyPlugin;
import lombok.Getter;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Model class for a configuration file.
 *
 * @since 1.0.0
 */
public final class ConfigWrapper {

    private final EconomyPlugin plugin;
    private final String fileName;
    private final Path path;
    private final HoconConfigurationLoader loader;

    @Getter
    private CommentedConfigurationNode node;

    /**
     * Constructs a new {@code ConfigWrapper} object.
     *
     * @param plugin   the Java plugin
     * @param fileName the config file name
     */
    public ConfigWrapper(final EconomyPlugin plugin, final String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.path = plugin.getDataPath().resolve(fileName);

        // Create loader
        this.loader = HoconConfigurationLoader.builder()
                .path(this.path)
                .prettyPrinting(true)
                .build();
    }

    /**
     * Synchronously loads the configuration from disk into the {@link #node}.
     */
    public void load() {
        // Check if file exists at path
        if (Files.notExists(this.path)) {
            this.plugin.getComponentLogger().info("Config file '{}' not found, creating it", this.fileName);
            this.plugin.saveResource(this.fileName, false);
        }

        // Load node using loader
        try {
            this.node = this.loader.load();
        } catch (final ConfigurateException e) {
            this.plugin.getComponentLogger().error("Failed to load config '{}'", this.fileName, e);

            if (this.node == null) {
                // Create node if missing
                this.node = this.loader.createNode();
            }
        }
    }

    /**
     * Persists the current in-memory {@link #node} state back to the disk.
     */
    public void save() {
        if (this.node == null) return;

        // Save node using loader
        try {
            this.loader.save(this.node);
        } catch (final ConfigurateException e) {
            this.plugin.getComponentLogger().error("Failed to save config '{}'", this.fileName, e);
        }
    }

}
