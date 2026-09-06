package com.insurance.portal.controller;

import com.insurance.portal.dto.request.PaymentRequest;
import com.insurance.portal.dto.response.PaymentResponse;
import com.insurance.portal.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Premium payments, invoices and receipts")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> pay(Authentication authentication, @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.makePayment(authentication.getName(), request));
    }

    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<PaymentResponse> invoice(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(paymentService.getInvoice(invoiceNumber));
    }

    @GetMapping("/{paymentId}/invoice")
    public ResponseEntity<PaymentResponse> invoiceByPaymentId(@PathVariable Long paymentId) {
        return ResponseEntity.ok(paymentService.getInvoiceByPaymentId(paymentId));
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id) {
        byte[] content = paymentService.generateReceipt(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt-" + id + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(content);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<PaymentResponse>> myPayments(Authentication authentication, Pageable pageable) {
        return ResponseEntity.ok(paymentService.listForCustomer(authentication.getName(), pageable));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<PaymentResponse>> paymentHistory(Authentication authentication, Pageable pageable) {
        return myPayments(authentication, pageable);
    }

    @GetMapping("/policy/{policyId}")
    public ResponseEntity<Page<PaymentResponse>> byPolicy(@PathVariable Long policyId, Pageable pageable) {
        return ResponseEntity.ok(paymentService.listByPolicy(policyId, pageable));
    }
}
