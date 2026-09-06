package com.insurance.portal.dto.response;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String category,
        String description,
        BigDecimal coverageAmount,
        BigDecimal premiumAmount,
        Integer tenureMonths,
        boolean active
) {
}
