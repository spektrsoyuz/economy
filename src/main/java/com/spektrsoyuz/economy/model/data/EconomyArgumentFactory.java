package com.spektrsoyuz.economy.model.data;

import com.spektrsoyuz.economy.model.account.Transactor;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.UUID;

/**
 * Argument factory for economy data.
 *
 * @since 1.0.0
 */
public final class EconomyArgumentFactory implements ArgumentFactory {

    @Override
    public Optional<Argument> build(final Type type, final Object value, final ConfigRegistry config) {
        // Null values
        if (value == null) {
            return Optional.empty();
        }

        // UUID
        if (type == UUID.class) {
            return Optional.of((position, statement, ctx) ->
                    statement.setString(position, value.toString()));
        }

        // Transactor
        if (type == Transactor.class) {
            return Optional.of((position, statement, ctx) ->
                    statement.setString(position, value.toString()));
        }

        return Optional.empty();
    }

}