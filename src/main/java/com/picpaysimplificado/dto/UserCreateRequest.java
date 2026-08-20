package com.picpaysimplificado.dto;

import com.picpaysimplificado.domain.user.UserType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UserCreateRequest(
        @NotBlank(message = "O primeiro nome é obrigatório")
        String firstName,

        @NotBlank(message = "O sobrenome é obrigatório")
        String lastName,

        @NotBlank(message = "O documento (CPF/CNPJ) é obrigatório")
        @Size(min = 11, max = 14, message = "O documento deve ter entre 11 e 14 caracteres")
        String document,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String password,

        @NotNull(message = "O saldo inicial é obrigatório")
        @DecimalMin(value = "0.00", message = "O saldo inicial não pode ser negativo")
        BigDecimal balance,

        @NotNull(message = "O tipo de usuário é obrigatório")
        UserType userType
) {}
