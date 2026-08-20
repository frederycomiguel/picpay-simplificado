package com.picpaysimplificado.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "O valor da transferência é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor mínimo de transferência é R$ 0,01")
        BigDecimal value,

        @NotNull(message = "O ID do pagador é obrigatório")
        Long payer,

        @NotNull(message = "O ID do recebedor é obrigatório")
        Long payee
) {}
