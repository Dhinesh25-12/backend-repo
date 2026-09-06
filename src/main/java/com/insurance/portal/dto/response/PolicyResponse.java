package com.insurance.portal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicyResponse(
        Long id,
        String policyNumber,
        Long customerId,
        String customerName,
        Long productId,
        String productName,
        String nomineeName,
        String nomineeRelationship,
        String nomineeContact,
        BigDecimal coverageAmount,
        BigDecimal premiumAmount,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        boolean cancellationRequested
) {
}
