package com.insurance.portal.repository;

import com.insurance.portal.entity.Policy;
import com.insurance.portal.entity.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByPolicyNumber(String policyNumber);
    Page<Policy> findByCustomerId(Long customerId, Pageable pageable);
    Page<Policy> findByAgentId(Long agentId, Pageable pageable);
    Page<Policy> findByStatus(PolicyStatus status, Pageable pageable);
    Page<Policy> findByCustomerIdAndStatus(Long customerId, PolicyStatus status, Pageable pageable);
    Page<Policy> findByCancellationRequestedTrue(Pageable pageable);
    long countByStatus(PolicyStatus status);

    @Query("SELECT p.status AS status, COUNT(p) AS total FROM Policy p GROUP BY p.status")
    List<PolicyStatusCount> countGroupedByStatus();

    interface PolicyStatusCount {
        PolicyStatus getStatus();
        long getTotal();
    }
    long countByCustomerIdAndStatus(Long customerId, PolicyStatus status);
    long countByCustomerId(Long customerId);

    @Query("SELECT p.customer.id AS customerId, COUNT(p) AS total FROM Policy p WHERE p.customer.id IN :customerIds GROUP BY p.customer.id")
    List<PolicyCustomerCount> countGroupedByCustomerIds(@Param("customerIds") Collection<Long> customerIds);

    interface PolicyCustomerCount {
        Long getCustomerId();
        long getTotal();
    }
    List<Policy> findTop5ByOrderByCreatedAtDesc();
}
