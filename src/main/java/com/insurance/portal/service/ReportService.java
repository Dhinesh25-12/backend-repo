package com.insurance.portal.service;

import com.insurance.portal.dto.response.AdminSummaryReport;
import com.insurance.portal.dto.response.MonthlyRevenueReport;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Report queries. The customer-facing reports use standard Spring Data JPA
 * queries (portable across databases and easy to unit test).
 * <p>
 * The admin-facing analytical reports demonstrate CTE + window-function based
 * SQL (see fn_top_customers_by_premium, payment_rollup_view, and
 * fn_claims_settlement_ratio in db/migration/V3__views_functions.sql) and are
 * executed as native queries against the PostgreSQL-specific view/function
 * objects created by Flyway.
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

    public AdminSummaryReport adminSummary() {
        BigDecimal totalPremium = paymentRepository.findAll().stream()
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalClaims = claimRepository.count();
        long settledClaims = claimRepository.findAll().stream()
                .filter(c -> c.getStatus() == ClaimStatus.SETTLED)
                .count();
        BigDecimal ratio = totalClaims == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(settledClaims * 100.0 / totalClaims).setScale(2, java.math.RoundingMode.HALF_UP);
        long totalPolicies = policyRepository.count();
        long activePolicies = policyRepository.findAll().stream()
                .filter(p -> p.getStatus() == PolicyStatus.ACTIVE)
                .count();
        return new AdminSummaryReport(totalPremium, totalClaims, settledClaims, ratio, totalPolicies, activePolicies);
    }

    /**
     * Calls the fn_top_customers_by_premium(limit) PL/pgSQL function which uses
     * a CTE + RANK() window function. Requires a PostgreSQL datasource.
     */
    @SuppressWarnings("unchecked")
    public List<TopCustomerReport> topCustomersByPremium(int limit) {
        List<Tuple> rows = entityManager
                .createNativeQuery("SELECT * FROM fn_top_customers_by_premium(:limitCount)", Tuple.class)
                .setParameter("limitCount", limit)
                .getResultList();
        return rows.stream().map(row -> new TopCustomerReport(
                ((Number) row.get("customer_id")).longValue(),
                (String) row.get("customer_name"),
                (BigDecimal) row.get("total_premium"),
                ((Number) row.get("customer_rank")).longValue()
        )).toList();
    }

    /**
     * Reads from payment_rollup_view for monthly revenue trend reporting.
     * Requires a PostgreSQL datasource.
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
     * Product-wise profitability/performance using a CTE-style join across
     * policy and payment tables.
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
                    SELECT pol.product_id, SUM(pay.amount) AS total_revenue
                    FROM payment pay
                    JOIN policy pol ON pol.id = pay.policy_id
                    WHERE pay.status = 'SUCCESS'
                    GROUP BY pol.product_id
                )
                SELECT p.id AS product_id, p.name AS product_name,
                       COALESCE(pp.policies_sold, 0) AS policies_sold,
                       COALESCE(pr.total_revenue, 0) AS total_revenue
                FROM product p
                LEFT JOIN product_policies pp ON pp.product_id = p.id
                LEFT JOIN product_revenue pr ON pr.product_id = p.id
                ORDER BY total_revenue DESC NULLS LAST
                """, Tuple.class).getResultList();
        return rows.stream().map(row -> new ProductPerformanceReport(
                ((Number) row.get("product_id")).longValue(),
                (String) row.get("product_name"),
                ((Number) row.get("policies_sold")).longValue(),
                (BigDecimal) row.get("total_revenue")
        )).toList();
    }

    private Customer requireCustomer(String username) {
        return customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + username));
    }
}
