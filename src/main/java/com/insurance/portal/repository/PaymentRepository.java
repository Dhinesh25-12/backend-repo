package com.insurance.portal.repository;

import com.insurance.portal.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByInvoiceNumber(String invoiceNumber);
    Page<Payment> findByCustomerId(Long customerId, Pageable pageable);
    Page<Payment> findByPolicyId(Long policyId, Pageable pageable);
}
