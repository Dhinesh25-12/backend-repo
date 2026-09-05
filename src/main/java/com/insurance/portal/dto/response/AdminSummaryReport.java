package com.insurance.portal.dto.response;

import java.math.BigDecimal;

public record AdminSummaryReport(
        BigDecimal totalPremiumCollected,
        Long totalClaims,
        Long settledClaims,
        BigDecimal claimsSettlementRatio,
        Long totalPolicies,
        Long activePolicies
) {
}
