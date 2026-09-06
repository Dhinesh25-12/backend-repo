package com.insurance.portal.repository;

import com.insurance.portal.entity.Policy;
import com.insurance.portal.entity.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT p.product.category AS category, COUNT(p) AS total FROM Policy p "
            + "WHERE p.status = :status GROUP BY p.product.category")
    List<PolicyCategoryCount> countGroupedByProductCategory(@Param("status") PolicyStatus status);

    interface PolicyCategoryCount {
        String getCategory();
        long getTotal();
    }
    long countByCustomerIdAndStatus(Long customerId, PolicyStatus status);
    List<Policy> findTop5ByOrderByCreatedAtDesc();
}
