package com.insurance.portal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        @NotBlank String category,
        String description,
        @NotNull @Positive BigDecimal coverageAmount,
        @NotNull @Positive BigDecimal premiumAmount,
        @NotNull @Positive Integer tenureMonths
) {
}
