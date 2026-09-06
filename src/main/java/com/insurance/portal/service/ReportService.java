package com.insurance.portal.service;

import com.insurance.portal.dto.response.AdminSummaryReport;
import com.insurance.portal.dto.response.ClaimsRatioReport;
import com.insurance.portal.dto.response.CustomerSummaryReport;
import com.insurance.portal.dto.response.MonthlyRevenueReport;
import com.insurance.portal.dto.response.PremiumCollectionReport;
import com.insurance.portal.dto.response.ProductPerformanceReport;
import com.insurance.portal.dto.response.TopCustomerReport;
import com.insurance.portal.entity.ClaimStatus;
import com.insurance.portal.entity.Customer;
import com.insurance.portal.entity.PolicyStatus;
import com.insurance.portal.exception.ResourceNotFoundException;
import com.insurance.portal.repository.ClaimRepository;
import com.insurance.portal.repository.CustomerRepository;
import com.insurance.portal.repository.PaymentRepository;
import com.insurance.portal.repository.PolicyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Report queries. The customer-facing reports use standard Spring Data JPA
 * queries (portable across databases and easy to unit test).
 * <p>
 * The admin-facing analytical reports read from payment_rollup_view (see
 * db/migration/V3__views_functions.sql, MySQL-compatible) or compute
 * aggregations directly in the JPA/service layer instead of relying on
 * PostgreSQL-only database functions.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final EntityManager entityManager;

    public Page<?> activePoliciesForCustomer(String username, Pageable pageable) {
        Customer customer = requireCustomer(username);
        return policyRepository.findByCustomerIdAndStatus(customer.getId(), PolicyStatus.ACTIVE, pageable);
    }

    public Page<?> expiredPoliciesForCustomer(String username, Pageable pageable) {
        Customer customer = requireCustomer(username);
        return policyRepository.findByCustomerIdAndStatus(customer.getId(), PolicyStatus.EXPIRED, pageable);
    }

    public Page<?> claimsSubmittedForCustomer(String username, Pageable pageable) {
        Customer customer = requireCustomer(username);
        return claimRepository.findByCustomerId(customer.getId(), pageable);
    }

    /**
     * Aggregate counts-only view of the customer-facing reports above, avoiding
     * the cost of fetching full pages of policy/claim entities.
     */
    public CustomerSummaryReport customerSummary(String username) {
        Customer customer = requireCustomer(username);
        long activePolicies = policyRepository.countByCustomerIdAndStatus(customer.getId(), PolicyStatus.ACTIVE);
        long expiredPolicies = policyRepository.countByCustomerIdAndStatus(customer.getId(), PolicyStatus.EXPIRED);
        long claimsSubmitted = claimRepository.countByCustomerId(customer.getId());
        return new CustomerSummaryReport(activePolicies, expiredPolicies, claimsSubmitted);
    }

    public AdminSummaryReport adminSummary() {
        BigDecimal totalPremium = paymentRepository.sumSuccessfulPaymentAmounts();
        long totalClaims = claimRepository.count();
        long settledClaims = claimRepository.countByStatus(ClaimStatus.SETTLED);
        BigDecimal ratio = totalClaims == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(settledClaims * 100.0 / totalClaims).setScale(2, RoundingMode.HALF_UP);
        long totalPolicies = policyRepository.count();
        long activePolicies = policyRepository.countByStatus(PolicyStatus.ACTIVE);
        return new AdminSummaryReport(totalPremium, totalClaims, settledClaims, ratio, totalPolicies, activePolicies);
    }

    /**
     * Ranks customers by total successful payment amount using a JPA
     * aggregation query (replaces the removed fn_top_customers_by_premium
     * PostgreSQL function, which has no MySQL equivalent).
     */
    public List<TopCustomerReport> topCustomersByPremium(int limit) {
        List<Object[]> rows = paymentRepository.aggregateTotalPremiumByCustomer(PageRequest.of(0, limit));
        List<TopCustomerReport> result = new ArrayList<>();
        long rank = 1;
        for (Object[] row : rows) {
            result.add(new TopCustomerReport(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (BigDecimal) row[2],
                    rank++
            ));
        }
        return result;
    }

    /**
     * Reads from payment_rollup_view for monthly revenue trend reporting.
     */
    @SuppressWarnings("unchecked")
    public List<MonthlyRevenueReport> monthlyRevenueTrend() {
        List<Tuple> rows = entityManager
                .createNativeQuery("SELECT revenue_month, payment_count, total_amount FROM payment_rollup_view", Tuple.class)
                .getResultList();
        return rows.stream().map(row -> new MonthlyRevenueReport(
                String.valueOf(row.get("revenue_month")),
                ((Number) row.get("payment_count")).longValue(),
                (BigDecimal) row.get("total_amount")
        )).toList();
    }

    /**
     * Same monthly premium collection data as {@link #monthlyRevenueTrend()},
     * reshaped to {period, totalCollected} field names.
     */
    public List<PremiumCollectionReport> premiumCollectionTrend() {
        return monthlyRevenueTrend().stream()
                .map(report -> new PremiumCollectionReport(report.month(), report.totalAmount()))
                .toList();
    }

    /**
     * Groups claims by incident month, counting total and settled claims per
     * period and computing the settlement ratio. Uses MySQL's DATE_FORMAT
     * (mirroring payment_rollup_view) instead of PostgreSQL-only functions.
     */
    @SuppressWarnings("unchecked")
    public List<ClaimsRatioReport> claimsRatioTrend() {
        List<Tuple> rows = entityManager.createNativeQuery("""
                SELECT DATE_FORMAT(incident_date, '%Y-%m-01') AS period,
                       COUNT(*) AS claims_filed,
                       SUM(CASE WHEN status = 'SETTLED' THEN 1 ELSE 0 END) AS claims_settled
                FROM claim
                GROUP BY DATE_FORMAT(incident_date, '%Y-%m-01')
                ORDER BY period
                """, Tuple.class).getResultList();
        return rows.stream().map(row -> {
            long filed = ((Number) row.get("claims_filed")).longValue();
            long settled = ((Number) row.get("claims_settled")).longValue();
            BigDecimal ratio = filed == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(settled * 100.0 / filed).setScale(2, RoundingMode.HALF_UP);
            return new ClaimsRatioReport(String.valueOf(row.get("period")), filed, settled, ratio);
        }).toList();
    }

    /**
     * Product-wise profitability/performance using a CTE-style join across
     * policy and payment tables. Uses MySQL-compatible "ORDER BY column IS
     * NULL, column" instead of the PostgreSQL-only "NULLS LAST" syntax.
     */
    @SuppressWarnings("unchecked")
    public List<ProductPerformanceReport> productPerformance() {
        List<Tuple> rows = entityManager.createNativeQuery("""
                WITH product_policies AS (
                    SELECT product_id, COUNT(*) AS policies_sold
                    FROM policy
                    GROUP BY product_id
                ),
                product_revenue AS (
                    SELECT pol.product_id, SUM(pay.amount) AS total_premium_collected
                    FROM payment pay
                    JOIN policy pol ON pol.id = pay.policy_id
                    WHERE pay.status = 'SUCCESS'
                    GROUP BY pol.product_id
                )
                SELECT p.id AS product_id, p.name AS product_name,
                       COALESCE(pp.policies_sold, 0) AS policies_sold,
                       COALESCE(pr.total_premium_collected, 0) AS total_premium_collected
                FROM product p
                LEFT JOIN product_policies pp ON pp.product_id = p.id
                LEFT JOIN product_revenue pr ON pr.product_id = p.id
                ORDER BY (total_premium_collected IS NULL), total_premium_collected DESC
                """, Tuple.class).getResultList();
        return rows.stream().map(row -> new ProductPerformanceReport(
                ((Number) row.get("product_id")).longValue(),
                (String) row.get("product_name"),
                ((Number) row.get("policies_sold")).longValue(),
                (BigDecimal) row.get("total_premium_collected")
        )).toList();
    }

    private Customer requireCustomer(String username) {
        return customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + username));
    }
}
