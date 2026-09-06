package com.insurance.portal.repository;

import com.insurance.portal.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByInvoiceNumber(String invoiceNumber);
    Page<Payment> findByCustomerId(Long customerId, Pageable pageable);
    Page<Payment> findByPolicyId(Long policyId, Pageable pageable);
    List<Payment> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = com.insurance.portal.entity.PaymentStatus.SUCCESS")
    BigDecimal sumSuccessfulPaymentAmounts();

    @Query("""
            SELECT c.id AS customerId,
                   CONCAT(u.firstName, ' ', u.lastName) AS customerName,
                   SUM(p.amount) AS totalPremium
            FROM Payment p
            JOIN p.customer c
            JOIN c.user u
            WHERE p.status = com.insurance.portal.entity.PaymentStatus.SUCCESS
            GROUP BY c.id, u.firstName, u.lastName
            ORDER BY SUM(p.amount) DESC
            """)
    List<Object[]> aggregateTotalPremiumByCustomer(Pageable pageable);
}
