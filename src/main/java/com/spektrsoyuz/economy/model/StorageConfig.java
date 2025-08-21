package com.spektrsoyuz.economy.model;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@Getter
@ConfigSerializable
public final class StorageConfig {

    private final String type = "mysql";
    private final String host = "127.0.0.1";
    private final int port = 3306;
    private final String database = "economy";
    private final String username = "username";
    private final String password = "password";
}
