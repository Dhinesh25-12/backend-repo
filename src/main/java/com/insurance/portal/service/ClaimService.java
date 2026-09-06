package com.insurance.portal.service;

import com.insurance.portal.dto.request.ClaimDecisionRequest;
import com.insurance.portal.dto.request.FileClaimRequest;
import com.insurance.portal.dto.response.ClaimResponse;
import com.insurance.portal.entity.*;
import com.insurance.portal.exception.BadRequestException;
import com.insurance.portal.exception.ResourceNotFoundException;
import com.insurance.portal.repository.AppUserRepository;
import com.insurance.portal.repository.ClaimRepository;
import com.insurance.portal.repository.CustomerRepository;
import com.insurance.portal.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public ClaimResponse fileClaim(String username, FileClaimRequest request) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + username));
        Policy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + request.policyId()));
        if (!policy.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Policy does not belong to the current user");
        }

        String claimNumber = StringUtils.hasText(request.claimNumber())
                ? request.claimNumber()
                : generateClaimNumber();

        Claim claim = Claim.builder()
                .claimNumber(claimNumber)
                .policy(policy)
                .customer(customer)
                .incidentDate(request.incidentDate())
                .description(request.description())
                .claimAmount(request.claimAmount())
                .documentUrl(request.documentUrl())
                .status(ClaimStatus.SUBMITTED)
                .build();

        claim = claimRepository.save(claim);
        return toResponse(claim);
    }

    @Transactional
    public ClaimResponse decideClaim(String officerUsername, Long claimId, ClaimDecisionRequest request) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimId));
        AppUser officer = appUserRepository.findByUsername(officerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + officerUsername));

        validateTransition(claim.getStatus(), request.status());

        claim.setStatus(request.status());
        claim.setDecisionNotes(request.decisionNotes());
        claim.setReviewedBy(officer);
        claimRepository.save(claim);
        return toResponse(claim);
    }

    public ClaimResponse getClaim(Long id) {
        return toResponse(claimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + id)));
    }

    public ClaimResponse getByClaimNumber(String claimNumber) {
        return toResponse(claimRepository.findByClaimNumber(claimNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found: " + claimNumber)));
    }
    @Transactional
    public Page<ClaimResponse> listForCustomer(String username, Pageable pageable) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + username));
        return claimRepository.findByCustomerId(customer.getId(), pageable).map(this::toResponse);
    }

    @Transactional
    public Page<ClaimResponse> listByCustomerId(Long customerId, Pageable pageable) {
        return claimRepository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    public Page<ClaimResponse> listByPolicy(Long policyId, Pageable pageable) {
        return claimRepository.findByPolicyId(policyId, pageable).map(this::toResponse);
    }
@Transactional
    public Page<ClaimResponse> queue(ClaimStatus status, Pageable pageable) {
        if (status != null) {
            return claimRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return claimRepository.findAll(pageable).map(this::toResponse);
    }

    private void validateTransition(ClaimStatus from, ClaimStatus to) {
        if (from == ClaimStatus.SETTLED || from == ClaimStatus.REJECTED) {
            throw new BadRequestException("Claim is already finalized and cannot be updated");
        }
        boolean valid = switch (from) {
            case SUBMITTED -> to == ClaimStatus.UNDER_REVIEW || to == ClaimStatus.REJECTED;
            case UNDER_REVIEW -> to == ClaimStatus.APPROVED || to == ClaimStatus.REJECTED;
            case APPROVED -> to == ClaimStatus.SETTLED;
            default -> false;
        };
        if (!valid) {
            throw new BadRequestException("Invalid claim status transition from " + from + " to " + to);
        }
    }

    private String generateClaimNumber() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String unique = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "CLM-" + year + "-" + unique;
    }

    private ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getClaimNumber(),
                claim.getPolicy().getId(),
                claim.getPolicy().getPolicyNumber(),
                claim.getCustomer().getId(),
                claim.getIncidentDate(),
                claim.getDescription(),
                claim.getClaimAmount(),
                claim.getStatus().name(),
                claim.getDocumentUrl(),
                claim.getDecisionNotes()
        );
    }
}
