package com.insurance.portal.dto.response;

public record CustomerSummaryReport(
        long activePolicies,
        long expiredPolicies,
        long claimsSubmitted
) {
}
