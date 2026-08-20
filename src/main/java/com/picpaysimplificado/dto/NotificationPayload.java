package com.picpaysimplificado.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationPayload(
        UUID transactionId,
        Long receiverId,
        String receiverEmail,
        String receiverName,
        String senderName,
        BigDecimal amount,
        LocalDateTime timestamp
) {}
