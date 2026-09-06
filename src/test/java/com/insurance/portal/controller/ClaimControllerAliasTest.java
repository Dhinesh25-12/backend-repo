package com.insurance.portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.portal.config.SecurityConfig;
import com.insurance.portal.dto.request.ClaimDecisionRequest;
import com.insurance.portal.dto.request.UpdateClaimStatusRequest;
import com.insurance.portal.dto.response.ClaimResponse;
import com.insurance.portal.entity.ClaimStatus;
import com.insurance.portal.security.CustomUserDetailsService;
import com.insurance.portal.security.JwtAuthenticationFilter;
import com.insurance.portal.security.JwtService;
import com.insurance.portal.service.ClaimService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ClaimControllerAliasTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClaimService claimService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private ClaimResponse sampleClaim() {
        return new ClaimResponse(1L, "CLM-2024-ABC", 1L, "POL-2024-ABC", 1L, LocalDate.now(),
                "Accident", BigDecimal.valueOf(1000), "SETTLED", null, "Approved");
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void fileAcceptsMultipartFormData() throws Exception {
        when(claimService.fileClaim(anyString(), any())).thenReturn(sampleClaim());

        mockMvc.perform(multipart("/api/claims")
                        .param("policyId", "1")
                        .param("incidentDate", "2024-01-01")
                        .param("description", "Accident")
                        .param("claimAmount", "1000"))
                .andExpect(status().isOk());

        verify(claimService).fileClaim(anyString(), any());
    }

    @Test
    @WithMockUser(username = "customer1", roles = "CUSTOMER")
    void myAliasDelegatesToListForCustomer() throws Exception {
        Page<ClaimResponse> page = new PageImpl<>(List.of(sampleClaim()));
        when(claimService.listForCustomer(anyString(), any())).thenReturn(page);

        mockMvc.perform(get("/api/claims/my"))
                .andExpect(status().isOk());

        verify(claimService).listForCustomer(anyString(), any());
    }

    @Test
    @WithMockUser(username = "officer1", roles = "CLAIMS_OFFICER")
    void patchStatusDelegatesToDecideClaimWithRemarksMappedToDecisionNotes() throws Exception {
        UpdateClaimStatusRequest request = new UpdateClaimStatusRequest(ClaimStatus.SETTLED, "Looks good");
        when(claimService.decideClaim(anyString(), anyLong(), any())).thenReturn(sampleClaim());

        mockMvc.perform(patch("/api/claims/1/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(claimService).decideClaim(anyString(), anyLong(),
                org.mockito.ArgumentMatchers.argThat(decision ->
                        decision.status() == ClaimStatus.SETTLED && "Looks good".equals(decision.decisionNotes())));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void patchStatusForbiddenForCustomer() throws Exception {
        UpdateClaimStatusRequest request = new UpdateClaimStatusRequest(ClaimStatus.SETTLED, "Looks good");
        mockMvc.perform(patch("/api/claims/1/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "agent1", roles = "AGENT")
    void agentCanViewClaimById() throws Exception {
        when(claimService.getClaim(1L)).thenReturn(sampleClaim());

        mockMvc.perform(get("/api/claims/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "agent1", roles = "AGENT")
    void agentCanViewClaimByClaimNumber() throws Exception {
        when(claimService.getByClaimNumber("CLM-2024-ABC")).thenReturn(sampleClaim());

        mockMvc.perform(get("/api/claims/number/CLM-2024-ABC"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "agent1", roles = "AGENT")
    void agentCanViewClaimsByPolicy() throws Exception {
        when(claimService.listByPolicy(anyLong(), any())).thenReturn(new PageImpl<>(List.of(sampleClaim())));

        mockMvc.perform(get("/api/claims/policy/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "agent1", roles = "AGENT")
    void agentCanViewClaimsQueue() throws Exception {
        when(claimService.queue(any(), any())).thenReturn(new PageImpl<>(List.of(sampleClaim())));

        mockMvc.perform(get("/api/claims/queue"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "agent1", roles = "AGENT")
    void decideForbiddenForAgent() throws Exception {
        ClaimDecisionRequest request = new ClaimDecisionRequest(ClaimStatus.APPROVED, "Approved");

        mockMvc.perform(put("/api/claims/1/decision")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "agent1", roles = "AGENT")
    void patchStatusForbiddenForAgent() throws Exception {
        UpdateClaimStatusRequest request = new UpdateClaimStatusRequest(ClaimStatus.SETTLED, "Looks good");

        mockMvc.perform(patch("/api/claims/1/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
