package com.insurance.portal.controller;

import com.insurance.portal.dto.response.AdminSummaryReport;
import com.insurance.portal.dto.response.ClaimsRatioReport;
import com.insurance.portal.dto.response.CustomerSummaryReport;
import com.insurance.portal.dto.response.MonthlyRevenueReport;
import com.insurance.portal.dto.response.PremiumCollectionReport;
import com.insurance.portal.dto.response.ProductPerformanceReport;
import com.insurance.portal.dto.response.TopCustomerReport;
import com.insurance.portal.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Customer and admin analytical reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/customer/active-policies")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<?>> activePolicies(Authentication authentication, Pageable pageable) {
        return ResponseEntity.ok(reportService.activePoliciesForCustomer(authentication.getName(), pageable));
    }

    @GetMapping("/customer/expired-policies")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<?>> expiredPolicies(Authentication authentication, Pageable pageable) {
        return ResponseEntity.ok(reportService.expiredPoliciesForCustomer(authentication.getName(), pageable));
    }

    @GetMapping("/customer/claims-submitted")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<?>> claimsSubmitted(Authentication authentication, Pageable pageable) {
        return ResponseEntity.ok(reportService.claimsSubmittedForCustomer(authentication.getName(), pageable));
    }

    @GetMapping("/customer/summary")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerSummaryReport> customerSummary(Authentication authentication) {
        return ResponseEntity.ok(reportService.customerSummary(authentication.getName()));
    }

    @GetMapping("/admin/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminSummaryReport> adminSummary() {
        return ResponseEntity.ok(reportService.adminSummary());
    }

    @GetMapping("/admin/top-customers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TopCustomerReport>> topCustomers(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(reportService.topCustomersByPremium(limit));
    }

    @GetMapping("/admin/monthly-revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MonthlyRevenueReport>> monthlyRevenue() {
        return ResponseEntity.ok(reportService.monthlyRevenueTrend());
    }

    @GetMapping("/admin/product-performance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProductPerformanceReport>> productPerformance() {
        return ResponseEntity.ok(reportService.productPerformance());
    }

    @GetMapping("/admin/premium-collection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PremiumCollectionReport>> premiumCollection() {
        return ResponseEntity.ok(reportService.premiumCollectionTrend());
    }

    @GetMapping("/admin/claims-ratio")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ClaimsRatioReport>> claimsRatio() {
        return ResponseEntity.ok(reportService.claimsRatioTrend());
    }
}
