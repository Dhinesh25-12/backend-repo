package com.insurance.portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.portal.config.SecurityConfig;
import com.insurance.portal.dto.request.ChangePasswordRequest;
import com.insurance.portal.exception.BadRequestException;
import com.insurance.portal.security.CustomUserDetailsService;
import com.insurance.portal.security.JwtAuthenticationFilter;
import com.insurance.portal.security.JwtService;
import com.insurance.portal.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserControllerPasswordTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(username = "cust1", roles = "CUSTOMER")
    void changePasswordReturnsNoContentOnSuccess() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "NewPass123");

        mockMvc.perform(post("/api/users/me/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(eq("cust1"), any(ChangePasswordRequest.class));
    }

    @Test
    @WithMockUser(username = "cust1", roles = "CUSTOMER")
    void changePasswordWithWrongCurrentPasswordReturnsBadRequest() throws Exception {
        doThrow(new BadRequestException("Invalid username or password"))
                .when(userService).changePassword(eq("cust1"), any(ChangePasswordRequest.class));
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPass", "NewPass123");

        mockMvc.perform(post("/api/users/me/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "cust1", roles = "CUSTOMER")
    void changePasswordWithWeakNewPasswordIsRejected() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "short");

        mockMvc.perform(post("/api/users/me/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    void changePasswordWithoutAuthenticationIsRejected() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("OldPass123", "NewPass123");

        mockMvc.perform(post("/api/users/me/password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
