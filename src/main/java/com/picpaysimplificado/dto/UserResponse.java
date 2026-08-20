package com.picpaysimplificado.dto;

import com.picpaysimplificado.domain.user.UserType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String document,
        String email,
        BigDecimal balance,
        UserType userType,
        LocalDateTime createdAt
) {}
