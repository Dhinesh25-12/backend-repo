package com.insurance.portal.dto.response;

import java.math.BigDecimal;

public record MonthlyRevenueReport(
        String month,
        Long paymentCount,
        BigDecimal totalAmount
) {
}
