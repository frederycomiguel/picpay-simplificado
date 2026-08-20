package com.picpaysimplificado.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        BigDecimal amount,
        Long payerId,
        String payerName,
        Long payeeId,
        String payeeName,
        String status,
        LocalDateTime timestamp
) {}
