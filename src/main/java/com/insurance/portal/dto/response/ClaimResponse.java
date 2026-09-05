package com.insurance.portal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClaimResponse(
        Long id,
        String claimNumber,
        Long policyId,
        String policyNumber,
        Long customerId,
        LocalDate incidentDate,
        String description,
        BigDecimal claimAmount,
        String status,
        String documentUrl,
        String decisionNotes
) {
}
