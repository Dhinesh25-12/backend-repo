package com.insurance.portal.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        long totalPolicies,
        long activePolicies,
        long claimsFiled,
        BigDecimal totalPayments,
        List<PolicySplitItem> policySplit,
        List<RevenueTrendItem> revenueTrend,
        List<ActivityItem> recentActivity
) {
}
