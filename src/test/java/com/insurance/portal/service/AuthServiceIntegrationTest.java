package com.insurance.portal.service;

import com.insurance.portal.dto.request.LoginRequest;
import com.insurance.portal.dto.request.RegisterRequest;
import com.insurance.portal.dto.response.AuthResponse;
import com.insurance.portal.entity.Role;
import com.insurance.portal.entity.RoleName;
import com.insurance.portal.exception.BadRequestException;
import com.insurance.portal.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void ensureRolesSeeded() {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName).orElseGet(() ->
                    roleRepository.save(Role.builder().name(roleName).description(roleName.name()).build()));
        }
    }

    @Test
    void registerThenLoginReturnsValidToken() {
        RegisterRequest registerRequest = new RegisterRequest(
                "janedoe", "janedoe@example.com", "Password123!", "Jane", "Doe", "+1-555-9999");

        AuthResponse registerResponse = authService.register(registerRequest);

        assertThat(registerResponse.token()).isNotBlank();
        assertThat(registerResponse.roles()).containsExactly("CUSTOMER");

        AuthResponse loginResponse = authService.login(new LoginRequest("janedoe", "Password123!"));
        assertThat(loginResponse.token()).isNotBlank();
        assertThat(loginResponse.username()).isEqualTo("janedoe");
    }

    @Test
    void registerWithDuplicateUsernameFails() {
        authService.register(new RegisterRequest("dupuser", "dup1@example.com", "Password123!", "Dup", "User", null));

        assertThrows(BadRequestException.class, () ->
                authService.register(new RegisterRequest("dupuser", "dup2@example.com", "Password123!", "Dup", "User", null)));
    }

    @Test
    void loginWithWrongPasswordFails() {
        authService.register(new RegisterRequest("wrongpassuser", "wrongpass@example.com", "Password123!", "Wrong", "Pass", null));

        assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("wrongpassuser", "IncorrectPass1")));
    }
}
