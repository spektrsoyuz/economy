package com.spektrsoyuz.economy.model;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public final class StorageConfig {

    private final String type;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    // Constructor
    public StorageConfig() {
        this.type = "mysql";
        this.host = "127.0.0.1";
        this.port = 3306;
        this.database = "economy";
        this.username = "username";
        this.password = "password";
    }
}
