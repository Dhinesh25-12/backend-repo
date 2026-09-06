package com.insurance.portal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FileClaimRequest(
        String claimNumber,
        @NotNull Long policyId,
        @NotNull LocalDate incidentDate,
        @NotBlank String description,
        BigDecimal claimAmount,
        String documentUrl
) {
}
