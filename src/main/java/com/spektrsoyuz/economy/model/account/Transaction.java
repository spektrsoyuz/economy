package com.spektrsoyuz.economy.model.account;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Model record for an economy transaction.
 *
 * @since 1.0.0
 */
public record Transaction(
        UUID accountId,
        String accountName,
        BigDecimal amount,
        Transactor transactor
) {

}
