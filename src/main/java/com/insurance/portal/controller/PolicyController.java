package com.insurance.portal.controller;

import com.insurance.portal.dto.request.CancelPolicyRequest;
import com.insurance.portal.dto.request.PurchasePolicyRequest;
import com.insurance.portal.dto.response.PolicyResponse;
import com.insurance.portal.dto.response.RenewalQuoteResponse;
import com.insurance.portal.entity.RoleName;
import com.insurance.portal.service.PolicyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@Tag(name = "Policy Management", description = "Purchase, renew, cancel policies")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping("/purchase")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> purchase(Authentication authentication,
                                                    @Valid @RequestBody PurchasePolicyRequest request) {
        return ResponseEntity.ok(policyService.purchasePolicy(authentication.getName(), request));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> purchaseAlias(Authentication authentication,
                                                         @Valid @RequestBody PurchasePolicyRequest request) {
        return purchase(authentication, request);
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> renew(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(policyService.renewPolicy(authentication.getName(), id));
    }

    @GetMapping("/{id}/renewal-quote")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<RenewalQuoteResponse> renewalQuote(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(policyService.previewRenewal(authentication.getName(), isAdmin(authentication), id));
    }

    private boolean isAdmin(Authentication authentication) {
        String adminAuthority = "ROLE_" + RoleName.ADMIN.name();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(adminAuthority));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> requestCancellation(Authentication authentication, @PathVariable Long id,
                                                               @Valid @RequestBody CancelPolicyRequest request) {
        return ResponseEntity.ok(policyService.requestCancellation(authentication.getName(), id, request));
    }

    @PostMapping("/{id}/cancellation-request")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PolicyResponse> requestCancellationAlias(Authentication authentication, @PathVariable Long id,
                                                                    @Valid @RequestBody CancelPolicyRequest request) {
        return requestCancellation(authentication, id, request);
    }

    @PostMapping("/{id}/cancel/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> approveCancellation(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.approveCancellation(id));
    }

    @PostMapping("/{id}/cancellation-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> approveCancellationAlias(@PathVariable Long id) {
        return approveCancellation(id);
    }

    @PostMapping("/{id}/cancellation-reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> rejectCancellation(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.rejectCancellation(id));
    }

    @GetMapping("/cancellation-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PolicyResponse>> cancellationRequests(Pageable pageable) {
        return ResponseEntity.ok(policyService.listCancellationRequests(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(policyService.getPolicy(id));
    }

    @GetMapping("/number/{policyNumber}")
    public ResponseEntity<PolicyResponse> getByPolicyNumber(@PathVariable String policyNumber) {
        return ResponseEntity.ok(policyService.getByPolicyNumber(policyNumber));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<PolicyResponse>> myPolicies(Authentication authentication, Pageable pageable) {
        return ResponseEntity.ok(policyService.listForCustomer(authentication.getName(), pageable));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<PolicyResponse>> myPoliciesAlias(Authentication authentication, Pageable pageable) {
        return myPolicies(authentication, pageable);
    }

    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasAnyRole('AGENT','ADMIN')")
    public ResponseEntity<Page<PolicyResponse>> byAgent(@PathVariable Long agentId, Pageable pageable) {
        return ResponseEntity.ok(policyService.listForAgent(agentId, pageable));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<PolicyResponse>> all(Pageable pageable) {
        return ResponseEntity.ok(policyService.listAll(pageable));
    }
}
