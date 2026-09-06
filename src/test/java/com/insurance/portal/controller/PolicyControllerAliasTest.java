package com.insurance.portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.portal.config.SecurityConfig;
import com.insurance.portal.dto.request.CancelPolicyRequest;
import com.insurance.portal.dto.request.PurchasePolicyRequest;
import com.insurance.portal.dto.response.PolicyResponse;
import com.insurance.portal.dto.response.RenewalQuoteResponse;
import com.insurance.portal.security.CustomUserDetailsService;
import com.insurance.portal.security.JwtAuthenticationFilter;
import com.insurance.portal.security.JwtService;
import com.insurance.portal.service.PolicyService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PolicyControllerAliasTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PolicyService policyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private PolicyResponse samplePolicy() {
        return new PolicyResponse(1L, "POL-2024-ABC", 1L, "Jane Doe", 1L, "Health Plan",
                "Nominee", "Spouse", "1234567890", BigDecimal.valueOf(100000), BigDecimal.valueOf(5000),
                LocalDate.now(), LocalDate.now().plusMonths(12), "ACTIVE", false);
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void purchaseAliasDelegatesToPurchasePolicy() throws Exception {
        PurchasePolicyRequest request = new PurchasePolicyRequest(1L, "Nominee", "Spouse", "1234567890");
        when(policyService.purchasePolicy(anyString(), any())).thenReturn(samplePolicy());

        mockMvc.perform(post("/api/policies")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(policyService).purchasePolicy(anyString(), any());
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void myAliasDelegatesToListForCustomer() throws Exception {
        Page<PolicyResponse> page = new PageImpl<>(List.of(samplePolicy()));
        when(policyService.listForCustomer(anyString(), any())).thenReturn(page);

        mockMvc.perform(get("/api/policies/my"))
                .andExpect(status().isOk());

        verify(policyService).listForCustomer(anyString(), any());
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void cancellationRequestAliasDelegatesToRequestCancellation() throws Exception {
        CancelPolicyRequest request = new CancelPolicyRequest("no longer needed");
        when(policyService.requestCancellation(anyString(), any(), any())).thenReturn(samplePolicy());

        mockMvc.perform(post("/api/policies/1/cancellation-request")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(policyService).requestCancellation(anyString(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancellationApproveAliasDelegatesToApproveCancellation() throws Exception {
        when(policyService.approveCancellation(any())).thenReturn(samplePolicy());

        mockMvc.perform(post("/api/policies/1/cancellation-approve"))
                .andExpect(status().isOk());

        verify(policyService).approveCancellation(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancellationRejectCallsRejectCancellation() throws Exception {
        when(policyService.rejectCancellation(any())).thenReturn(samplePolicy());

        mockMvc.perform(post("/api/policies/1/cancellation-reject"))
                .andExpect(status().isOk());

        verify(policyService).rejectCancellation(any());
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void renewalQuoteReturnsPreview() throws Exception {
        RenewalQuoteResponse quote = new RenewalQuoteResponse(1L, BigDecimal.valueOf(5000), LocalDate.now().plusMonths(12));
        when(policyService.previewRenewal(anyString(), org.mockito.ArgumentMatchers.eq(false), any())).thenReturn(quote);

        mockMvc.perform(get("/api/policies/1/renewal-quote"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void cancellationRequestsQueueRequiresAdmin() throws Exception {
        Page<PolicyResponse> page = new PageImpl<>(List.of(samplePolicy()));
        when(policyService.listCancellationRequests(any())).thenReturn(page);

        mockMvc.perform(get("/api/policies/cancellation-requests"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void cancellationRequestsQueueForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/api/policies/cancellation-requests"))
                .andExpect(status().isForbidden());
    }
}
