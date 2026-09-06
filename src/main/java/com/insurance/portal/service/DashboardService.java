package com.insurance.portal.service;

import com.insurance.portal.dto.response.ActivityItem;
import com.insurance.portal.dto.response.DashboardSummaryResponse;
import com.insurance.portal.dto.response.MonthlyRevenueReport;
import com.insurance.portal.dto.response.PolicySplitItem;
import com.insurance.portal.dto.response.RevenueTrendItem;
import com.insurance.portal.entity.Claim;
import com.insurance.portal.entity.Payment;
import com.insurance.portal.entity.Policy;
import com.insurance.portal.entity.PolicyStatus;
import com.insurance.portal.repository.ClaimRepository;
import com.insurance.portal.repository.PaymentRepository;
import com.insurance.portal.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Aggregates existing policy/claim/payment/report data into a single dashboard
 * summary payload, mirroring the same data sources used by
 * {@link ReportService#adminSummary()} and {@link ReportService#monthlyRevenueTrend()}.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_ACTIVITY_LIMIT = 5;

    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;
    private final ReportService reportService;

    public DashboardSummaryResponse summary() {
        long totalPolicies = policyRepository.count();
        long activePolicies = policyRepository.countByStatus(PolicyStatus.ACTIVE);
        long claimsFiled = claimRepository.count();
        var totalPayments = paymentRepository.sumSuccessfulPaymentAmounts();

        List<PolicySplitItem> policySplit = buildPolicySplit();

        List<RevenueTrendItem> revenueTrend = reportService.monthlyRevenueTrend().stream()
                .map(this::toRevenueTrendItem)
                .toList();

        List<ActivityItem> recentActivity = recentActivity();

        return new DashboardSummaryResponse(totalPolicies, activePolicies, claimsFiled, totalPayments,
                policySplit, revenueTrend, recentActivity);
    }

    private RevenueTrendItem toRevenueTrendItem(MonthlyRevenueReport report) {
        long revenue = report.totalAmount() == null ? 0L : report.totalAmount().longValue();
        return RevenueTrendItem.builder().month(report.month()).revenue(revenue).build();
    }

    private List<PolicySplitItem> buildPolicySplit() {
        java.util.Map<PolicyStatus, Long> counts = new java.util.EnumMap<>(PolicyStatus.class);
        for (PolicyStatus status : PolicyStatus.values()) {
            counts.put(status, 0L);
        }
        for (PolicyRepository.PolicyStatusCount row : policyRepository.countGroupedByStatus()) {
            counts.put(row.getStatus(), row.getTotal());
        }
        List<PolicySplitItem> policySplit = new ArrayList<>();
        for (PolicyStatus status : PolicyStatus.values()) {
            policySplit.add(PolicySplitItem.builder()
                    .category(status.name())
                    .count(counts.get(status).intValue())
                    .build());
        }
        return policySplit;
    }

    private List<ActivityItem> recentActivity() {
        record Event(LocalDateTime timestamp, String message) {
        }

        List<Event> events = new ArrayList<>();
        for (Policy policy : policyRepository.findTop5ByOrderByCreatedAtDesc()) {
            events.add(new Event(policy.getCreatedAt(),
                    "Policy " + policy.getPolicyNumber() + " is " + policy.getStatus().name()));
        }
        for (Claim claim : claimRepository.findTop5ByOrderByCreatedAtDesc()) {
            events.add(new Event(claim.getCreatedAt(),
                    "Claim " + claim.getClaimNumber() + " is " + claim.getStatus().name()));
        }
        for (Payment payment : paymentRepository.findTop5ByOrderByCreatedAtDesc()) {
            events.add(new Event(payment.getCreatedAt(),
                    "Payment of " + payment.getAmount() + " received for policy " + payment.getPolicy().getPolicyNumber()));
        }

        return events.stream()
                .sorted(Comparator.comparing(Event::timestamp, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_ACTIVITY_LIMIT)
                .map(event -> ActivityItem.builder()
                        .message(event.message())
                        .timestamp(String.valueOf(event.timestamp()))
                        .build())
                .toList();
    }
}
