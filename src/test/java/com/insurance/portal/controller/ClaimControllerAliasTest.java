package com.insurance.portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.portal.config.SecurityConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
}
