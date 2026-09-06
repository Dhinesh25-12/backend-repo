package com.insurance.portal.dto.response;

import java.math.BigDecimal;

public record ClaimsRatioReport(
        String period,
        Long claimsFiled,
        Long claimsSettled,
        BigDecimal settlementRatio
) {
}
