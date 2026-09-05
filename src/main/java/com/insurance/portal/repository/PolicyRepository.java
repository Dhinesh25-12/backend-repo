package com.insurance.portal.repository;

import com.insurance.portal.entity.Policy;
import com.insurance.portal.entity.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByPolicyNumber(String policyNumber);
    Page<Policy> findByCustomerId(Long customerId, Pageable pageable);
    Page<Policy> findByAgentId(Long agentId, Pageable pageable);
    Page<Policy> findByStatus(PolicyStatus status, Pageable pageable);
    Page<Policy> findByCustomerIdAndStatus(Long customerId, PolicyStatus status, Pageable pageable);
    long countByStatus(PolicyStatus status);
}
