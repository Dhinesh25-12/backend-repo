package com.insurance.portal.dto.response;

import java.math.BigDecimal;

public record ProductPerformanceReport(
        Long productId,
        String productName,
        Long policiesSold,
        BigDecimal totalPremiumCollected
) {
}
