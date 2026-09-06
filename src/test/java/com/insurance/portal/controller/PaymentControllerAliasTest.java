package com.insurance.portal.controller;

import com.insurance.portal.config.SecurityConfig;
import com.insurance.portal.dto.response.PaymentResponse;
import com.insurance.portal.security.CustomUserDetailsService;
import com.insurance.portal.security.JwtAuthenticationFilter;
import com.insurance.portal.security.JwtService;
import com.insurance.portal.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PaymentControllerAliasTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private PaymentResponse samplePayment() {
        return new PaymentResponse(1L, 1L, "POL-2024-ABC", BigDecimal.valueOf(5000), LocalDateTime.now(),
                "CARD", "SUCCESS", "INV-2024-ABC", "RCPT-2024-ABC");
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void historyAliasDelegatesToListForCustomer() throws Exception {
        Page<PaymentResponse> page = new PageImpl<>(List.of(samplePayment()));
        when(paymentService.listForCustomer(anyString(), any())).thenReturn(page);

        mockMvc.perform(get("/api/payments/history"))
                .andExpect(status().isOk());

        verify(paymentService).listForCustomer(anyString(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void invoiceByPaymentIdDelegatesToGetInvoiceByPaymentId() throws Exception {
        when(paymentService.getInvoiceByPaymentId(anyLong())).thenReturn(samplePayment());

        mockMvc.perform(get("/api/payments/1/invoice"))
                .andExpect(status().isOk());

        verify(paymentService).getInvoiceByPaymentId(anyLong());
    }
}
