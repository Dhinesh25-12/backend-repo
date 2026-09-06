package com.insurance.portal.dto.response;

import java.math.BigDecimal;

public record PremiumCollectionReport(
        String period,
        BigDecimal totalCollected
) {
}
