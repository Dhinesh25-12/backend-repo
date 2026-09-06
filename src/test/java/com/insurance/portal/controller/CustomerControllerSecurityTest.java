package com.insurance.portal.controller;

import com.insurance.portal.config.SecurityConfig;
import com.insurance.portal.dto.response.CustomerResponse;
import com.insurance.portal.security.CustomUserDetailsService;
import com.insurance.portal.security.JwtAuthenticationFilter;
import com.insurance.portal.security.JwtService;
import com.insurance.portal.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class CustomerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomerResponse sampleCustomer() {
        return new CustomerResponse(1L, 7L, "jdoe", "jdoe@example.com", "John", "Doe", "John Doe",
                "555", true, LocalDate.of(1990, 1, 2), "1 Main St", "Springfield", "IL", "62701",
                "PASSPORT", "X123", 2L);
    }

    @Test
    void listCustomersWithoutAuthenticationIsRejected() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void listCustomersAsCustomerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLAIMS_OFFICER")
    void listCustomersAsClaimsOfficerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void listCustomersAsAgentIsAllowed() throws Exception {
        when(customerService.listCustomers(any(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/customers")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void customerDetailAsAdminIsAllowed() throws Exception {
        when(customerService.getCustomer(anyLong())).thenReturn(sampleCustomer());
        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jdoe"))
                .andExpect(jsonPath("$.kycIdNumber").value("X123"));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void customerPoliciesAsAgentIsAllowed() throws Exception {
        when(customerService.listPolicies(anyLong(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/customers/1/policies")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void customerClaimsAsAgentIsAllowed() throws Exception {
        when(customerService.listClaims(anyLong(), any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/customers/1/claims")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerClaimsAsCustomerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/customers/1/claims")).andExpect(status().isForbidden());
    }
}
