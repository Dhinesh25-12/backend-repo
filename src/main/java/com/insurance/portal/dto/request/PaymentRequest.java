package com.insurance.portal.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull Long policyId,
        @NotNull @Positive BigDecimal amount,
        String paymentMethod
) {
}
