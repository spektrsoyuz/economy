package com.spektrsoyuz.economy.model.data;

import com.spektrsoyuz.economy.model.account.Transactor;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.UUID;

public class EconomyArgumentFactory implements ArgumentFactory {

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        if (value == null) {
            return Optional.empty();
        }

        // Handle UUIDs
        if (type == UUID.class) {
            return Optional.of((position, statement, ctx) ->
                    statement.setString(position, value.toString()));
        }

        // Handle your custom Transactor class/enum
        if (type == Transactor.class) {
            return Optional.of((position, statement, ctx) ->
                    statement.setString(position, value.toString()));
        }

        return Optional.empty();
    }
}