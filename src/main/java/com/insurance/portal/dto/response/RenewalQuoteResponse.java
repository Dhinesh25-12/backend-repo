package com.insurance.portal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RenewalQuoteResponse(
        Long policyId,
        BigDecimal renewalPremium,
        LocalDate newEndDate
) {
}
