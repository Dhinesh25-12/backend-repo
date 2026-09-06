package com.insurance.portal.controller;

import com.insurance.portal.config.SecurityConfig;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        when(customerService.getCustomer(anyLong())).thenReturn(null);
        mockMvc.perform(get("/api/customers/1")).andExpect(status().isOk());
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
