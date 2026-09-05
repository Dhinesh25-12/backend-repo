package com.insurance.portal.service;

import com.insurance.portal.dto.request.PaymentRequest;
import com.insurance.portal.dto.response.PaymentResponse;
import com.insurance.portal.entity.Customer;
import com.insurance.portal.entity.Payment;
import com.insurance.portal.entity.PaymentStatus;
import com.insurance.portal.entity.Policy;
import com.insurance.portal.exception.BadRequestException;
import com.insurance.portal.exception.ResourceNotFoundException;
import com.insurance.portal.repository.CustomerRepository;
import com.insurance.portal.repository.PaymentRepository;
import com.insurance.portal.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public PaymentResponse makePayment(String username, PaymentRequest request) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + username));
        Policy policy = policyRepository.findById(request.policyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found: " + request.policyId()));
        if (!policy.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Policy does not belong to the current user");
        }

        String invoiceNumber = generateNumber("INV");
        String receiptNumber = generateNumber("RCPT");

        Payment payment = Payment.builder()
                .policy(policy)
                .customer(customer)
                .amount(request.amount())
                .paymentMethod(StringUtils.hasText(request.paymentMethod()) ? request.paymentMethod() : "CARD")
                .status(PaymentStatus.SUCCESS)
                .invoiceNumber(invoiceNumber)
                .receiptNumber(receiptNumber)
                .build();

        payment = paymentRepository.save(payment);
        return toResponse(payment);
    }

    public PaymentResponse getInvoice(String invoiceNumber) {
        return toResponse(paymentRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceNumber)));
    }

    public byte[] generateReceipt(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        String receipt = """
                ===================================
                INSURANCE POLICY MANAGEMENT PORTAL
                PAYMENT RECEIPT
                ===================================
                Receipt Number : %s
                Invoice Number : %s
                Policy Number  : %s
                Payment Date   : %s
                Amount Paid    : %s
                Payment Method : %s
                Status         : %s
                ===================================
                Thank you for your payment.
                """.formatted(
                payment.getReceiptNumber(),
                payment.getInvoiceNumber(),
                payment.getPolicy().getPolicyNumber(),
                payment.getPaymentDate(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus()
        );
        return receipt.getBytes(StandardCharsets.UTF_8);
    }

    public Page<PaymentResponse> listForCustomer(String username, Pageable pageable) {
        Customer customer = customerRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Customer profile not found for user: " + username));
        return paymentRepository.findByCustomerId(customer.getId(), pageable).map(this::toResponse);
    }

    public Page<PaymentResponse> listByPolicy(Long policyId, Pageable pageable) {
        return paymentRepository.findByPolicyId(policyId, pageable).map(this::toResponse);
    }

    private String generateNumber(String prefix) {
        long count = paymentRepository.count() + 1;
        String year = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy"));
        return prefix + "-" + year + "-" + String.format("%06d", count) + "-" + System.nanoTime() % 1000;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPolicy().getId(),
                payment.getPolicy().getPolicyNumber(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                payment.getStatus().name(),
                payment.getInvoiceNumber(),
                payment.getReceiptNumber()
        );
    }
}
