package com.insurance.portal.controller;

import com.insurance.portal.dto.response.ClaimResponse;
import com.insurance.portal.dto.response.CustomerResponse;
import com.insurance.portal.dto.response.PolicyResponse;
import com.insurance.portal.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Agent and admin access to customer records")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<Page<CustomerResponse>> list(@RequestParam(required = false) String query,
                                                       Pageable pageable) {
        return ResponseEntity.ok(customerService.listCustomers(query, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<CustomerResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @GetMapping("/{id}/policies")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<Page<PolicyResponse>> policies(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(customerService.listPolicies(id, pageable));
    }

    @GetMapping("/{id}/claims")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<Page<ClaimResponse>> claims(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(customerService.listClaims(id, pageable));
    }
}
