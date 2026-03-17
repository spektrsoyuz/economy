package com.spektrsoyuz.economy.model;

import java.math.BigDecimal;
import java.util.UUID;

public record Transaction(
        UUID accountId,
        String accountName,
        BigDecimal amount,
        Transactor transactor
) {

}
