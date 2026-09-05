package com.insurance.portal.service;

import com.insurance.portal.dto.request.CancelPolicyRequest;
import com.insurance.portal.dto.request.PurchasePolicyRequest;
import com.insurance.portal.dto.response.PolicyResponse;
import com.insurance.portal.entity.*;
import com.insurance.portal.exception.BadRequestException;
import com.insurance.portal.exception.ResourceNotFoundException;
import com.insurance.portal.repository.CustomerRepository;
import com.insurance.portal.repository.PolicyRepository;
import com.insurance.portal.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public PolicyResponse purchasePolicy(String username, PurchasePolicyRequest request) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + username));
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
        if (!product.isActive()) {
            throw new BadRequestException("Product is not available for purchase");
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(product.getTenureMonths());

        Policy policy = Policy.builder()
                .policyNumber(generatePolicyNumber())
                .customer(customer)
                .product(product)
                .nomineeName(request.nomineeName())
                .nomineeRelationship(request.nomineeRelationship())
                .nomineeContact(request.nomineeContact())
                .coverageAmount(product.getCoverageAmount())
                .premiumAmount(product.getPremiumAmount())
                .startDate(startDate)
                .endDate(endDate)
                .status(PolicyStatus.ACTIVE)
                .build();

        policy = policyRepository.save(policy);
        return toResponse(policy);
    }

    @Transactional
    public Map<String, Object> renewPolicy(String username, Long policyId) {
        Policy policy = findOwnedPolicy(username, policyId);
        if (policy.getStatus() == PolicyStatus.CANCELLED) {
            throw new BadRequestException("Cannot renew a cancelled policy");
        }
        LocalDate newStart = policy.getEndDate().isAfter(LocalDate.now()) ? policy.getEndDate() : LocalDate.now();
        LocalDate newEnd = newStart.plusMonths(policy.getProduct().getTenureMonths());
        policy.setStartDate(newStart);
        policy.setEndDate(newEnd);
        policy.setStatus(PolicyStatus.ACTIVE);
        policyRepository.save(policy);

        return Map.of(
                "policy", toResponse(policy),
                "renewalPremium", policy.getPremiumAmount()
        );
    }

    @Transactional
    public PolicyResponse requestCancellation(String username, Long policyId, CancelPolicyRequest request) {
        Policy policy = findOwnedPolicy(username, policyId);
        policy.setCancellationRequested(true);
        policy.setCancellationReason(request.reason());
        policy.setStatus(PolicyStatus.CANCELLATION_REQUESTED);
        policyRepository.save(policy);
        return toResponse(policy);
    }

    @Transactional
    public PolicyResponse approveCancellation(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        if (!policy.isCancellationRequested()) {
            throw new BadRequestException("No cancellation request pending for this policy");
        }
        policy.setStatus(PolicyStatus.CANCELLED);
        policyRepository.save(policy);
        return toResponse(policy);
    }

    public PolicyResponse getPolicy(Long id) {
        return toResponse(policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + id)));
    }

    public PolicyResponse getByPolicyNumber(String policyNumber) {
        return toResponse(policyRepository.findByPolicyNumber(policyNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyNumber)));
    }

    public Page<PolicyResponse> listForCustomer(String username, Pageable pageable) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + username));
        return policyRepository.findByCustomerId(customer.getId(), pageable).map(this::toResponse);
    }

    public Page<PolicyResponse> listForAgent(Long agentId, Pageable pageable) {
        return policyRepository.findByAgentId(agentId, pageable).map(this::toResponse);
    }

    public Page<PolicyResponse> listAll(Pageable pageable) {
        return policyRepository.findAll(pageable).map(this::toResponse);
    }

    private Policy findOwnedPolicy(String username, Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + policyId));
        if (!policy.getCustomer().getUser().getUsername().equals(username)) {
            throw new BadRequestException("Policy does not belong to the current user");
        }
        return policy;
    }

    private String generatePolicyNumber() {
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        String unique = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "POL-" + year + "-" + unique;
    }

    private PolicyResponse toResponse(Policy policy) {
        AppUser user = policy.getCustomer().getUser();
        return new PolicyResponse(
                policy.getId(),
                policy.getPolicyNumber(),
                policy.getCustomer().getId(),
                user.getFirstName() + " " + user.getLastName(),
                policy.getProduct().getId(),
                policy.getProduct().getName(),
                policy.getNomineeName(),
                policy.getNomineeRelationship(),
                policy.getNomineeContact(),
                policy.getCoverageAmount(),
                policy.getPremiumAmount(),
                policy.getStartDate(),
                policy.getEndDate(),
                policy.getStatus().name(),
                policy.isCancellationRequested()
        );
    }
}
