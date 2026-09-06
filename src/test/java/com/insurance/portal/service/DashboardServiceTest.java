package com.insurance.portal.service;

import com.insurance.portal.dto.response.DashboardSummaryResponse;
import com.insurance.portal.dto.response.MonthlyRevenueReport;
import com.insurance.portal.dto.response.PolicySplitItem;
import com.insurance.portal.entity.PolicyStatus;
import com.insurance.portal.repository.ClaimRepository;
import com.insurance.portal.repository.PaymentRepository;
import com.insurance.portal.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReportService reportService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(policyRepository, claimRepository, paymentRepository, reportService);
    }

    @Test
    void summary_groupsPolicySplitByProductCategoryNotStatus() {
        when(policyRepository.count()).thenReturn(4L);
        when(policyRepository.countByStatus(PolicyStatus.ACTIVE)).thenReturn(4L);
        when(claimRepository.count()).thenReturn(0L);
        when(paymentRepository.sumSuccessfulPaymentAmounts()).thenReturn(BigDecimal.ZERO);
        when(policyRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(claimRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(paymentRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(reportService.monthlyRevenueTrend()).thenReturn(List.<MonthlyRevenueReport>of());

        when(policyRepository.countGroupedByProductCategory(PolicyStatus.ACTIVE)).thenReturn(List.of(
                categoryCount("Health Insurance", 2L),
                categoryCount("Motor Insurance", 1L),
                categoryCount("Life Insurance", 1L)
        ));

        DashboardSummaryResponse summary = dashboardService.summary();

        List<PolicySplitItem> policySplit = summary.policySplit();
        assertThat(policySplit).hasSize(3);
        assertThat(policySplit).extracting(PolicySplitItem::getCategory)
                .containsExactlyInAnyOrder("Health Insurance", "Motor Insurance", "Life Insurance");
        assertThat(policySplit).noneMatch(item ->
                item.getCategory().equals("ACTIVE") || item.getCategory().equals("EXPIRED")
                        || item.getCategory().equals("CANCELLED"));

        PolicySplitItem health = policySplit.stream()
                .filter(item -> item.getCategory().equals("Health Insurance"))
                .findFirst().orElseThrow();
        assertThat(health.getCount()).isEqualTo(2);
    }

    private PolicyRepository.PolicyCategoryCount categoryCount(String category, long total) {
        return new PolicyRepository.PolicyCategoryCount() {
            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
