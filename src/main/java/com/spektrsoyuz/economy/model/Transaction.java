package com.spektrsoyuz.economy.model;

import java.math.BigDecimal;
import java.util.UUID;

// Model class for an economy transaction
public record Transaction(
        UUID accountId,
        String accountName,
        String currency,
        BigDecimal amount,
        Transactor transactor
) {

}
