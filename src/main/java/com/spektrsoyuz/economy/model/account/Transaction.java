package com.spektrsoyuz.economy.model.account;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Model record for an economy transaction.
 *
 * @param accountId   The UUID of the account affected.
 * @param accountName The name of the account at the time of the transaction.
 * @param amount      The amount changed (positive for deposits, negative for withdrawals).
 * @param transactor  The entity or actor that initiated the transaction.
 * @since 1.0.0
 */
public record Transaction(
        UUID accountId,
        String accountName,
        BigDecimal amount,
        Transactor transactor
) {

}
