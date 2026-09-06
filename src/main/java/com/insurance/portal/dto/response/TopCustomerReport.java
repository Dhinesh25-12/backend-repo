package com.insurance.portal.dto.response;

import java.math.BigDecimal;

public record TopCustomerReport(
        Long customerId,
        String customerName,
        BigDecimal totalPremium,
        Long rank
) {
}
