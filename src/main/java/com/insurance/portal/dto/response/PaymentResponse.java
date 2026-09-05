package com.insurance.portal.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long policyId,
        String policyNumber,
        BigDecimal amount,
        LocalDateTime paymentDate,
        String paymentMethod,
        String status,
        String invoiceNumber,
        String receiptNumber
) {
}
