package com.insurance.portal.controller;

import com.insurance.portal.dto.request.ClaimDecisionRequest;
import com.insurance.portal.dto.request.FileClaimRequest;
import com.insurance.portal.dto.request.UpdateClaimStatusRequest;
import com.insurance.portal.dto.response.ClaimResponse;
import com.insurance.portal.entity.ClaimStatus;
import com.insurance.portal.service.ClaimService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Tag(name = "Claims Management", description = "File and process insurance claims")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ClaimResponse> file(Authentication authentication, @Valid @RequestBody FileClaimRequest request) {
        return ResponseEntity.ok(claimService.fileClaim(authentication.getName(), request));
    }

    @PutMapping("/{id}/decision")
    @PreAuthorize("hasRole('CLAIMS_OFFICER')")
    public ResponseEntity<ClaimResponse> decide(Authentication authentication, @PathVariable Long id,
                                                 @Valid @RequestBody ClaimDecisionRequest request) {
        return ResponseEntity.ok(claimService.decideClaim(authentication.getName(), id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('CLAIMS_OFFICER')")
    public ResponseEntity<ClaimResponse> updateStatus(Authentication authentication, @PathVariable Long id,
                                                       @Valid @RequestBody UpdateClaimStatusRequest request) {
        ClaimDecisionRequest decision = new ClaimDecisionRequest(request.status(), request.remarks());
        return ResponseEntity.ok(claimService.decideClaim(authentication.getName(), id, decision));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClaimResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(claimService.getClaim(id));
    }

    @GetMapping("/number/{claimNumber}")
    public ResponseEntity<ClaimResponse> getByClaimNumber(@PathVariable String claimNumber) {
        return ResponseEntity.ok(claimService.getByClaimNumber(claimNumber));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<ClaimResponse>> myClaims(Authentication authentication, Pageable pageable) {
        return ResponseEntity.ok(claimService.listForCustomer(authentication.getName(), pageable));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<ClaimResponse>> myClaimsAlias(Authentication authentication, Pageable pageable) {
        return myClaims(authentication, pageable);
    }

    @GetMapping("/policy/{policyId}")
    public ResponseEntity<Page<ClaimResponse>> byPolicy(@PathVariable Long policyId, Pageable pageable) {
        return ResponseEntity.ok(claimService.listByPolicy(policyId, pageable));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'AGENT')")
    public ResponseEntity<Page<ClaimResponse>> queue(@RequestParam(required = false) ClaimStatus status, Pageable pageable) {
        return ResponseEntity.ok(claimService.queue(status, pageable));
    }
}
