package com.insurance.portal.repository;

import com.insurance.portal.entity.Claim;
import com.insurance.portal.entity.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimNumber(String claimNumber);
    Page<Claim> findByCustomerId(Long customerId, Pageable pageable);
    Page<Claim> findByPolicyId(Long policyId, Pageable pageable);
    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);
}
