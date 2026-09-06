package com.insurance.portal.service;

import com.insurance.portal.dto.request.LoginRequest;
import com.insurance.portal.dto.request.RegisterRequest;
import com.insurance.portal.dto.response.AuthResponse;
import com.insurance.portal.entity.AppUser;
import com.insurance.portal.entity.Role;
import com.insurance.portal.entity.RoleName;
import com.insurance.portal.repository.AppUserRepository;
import com.insurance.portal.repository.CustomerRepository;
import com.insurance.portal.repository.RoleRepository;
import com.insurance.portal.security.JwtService;
import com.insurance.portal.security.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(appUserRepository, customerRepository, roleRepository,
                passwordEncoder, jwtService, authenticationManager);

        customerRole = Role.builder().id(1L).name(RoleName.CUSTOMER).build();
    }

    @Test
    void registerReturnsNestedUserSummaryAndExpiresIn() {
        when(appUserRepository.existsByUsername("janedoe")).thenReturn(false);
        when(appUserRepository.existsByEmail("janedoe@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(appUserRepository.save(any())).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(jwtService.generateToken(any(SecurityUser.class))).thenReturn("test-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        RegisterRequest request = new RegisterRequest(
                "janedoe", "janedoe@example.com", "Password123!", "Jane", "Doe", "+1-555-9999");

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo(42L);
        assertThat(response.user().username()).isEqualTo("janedoe");
        assertThat(response.user().email()).isEqualTo("janedoe@example.com");
        assertThat(response.user().fullName()).isEqualTo("Jane Doe");
        assertThat(response.user().roles()).containsExactly("CUSTOMER");
        assertThat(response.user().enabled()).isTrue();
    }

    @Test
    void loginReturnsNestedUserSummaryAndExpiresIn() {
        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);
        AppUser user = AppUser.builder()
                .id(7L)
                .username("johndoe")
                .email("johndoe@example.com")
                .firstName("John")
                .lastName("Doe")
                .active(true)
                .roles(roles)
                .build();

        when(appUserRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(SecurityUser.class))).thenReturn("login-token");
        when(jwtService.getExpirationSeconds()).thenReturn(1800L);

        AuthResponse response = authService.login(new LoginRequest("johndoe", "Password123!"));

        assertThat(response.token()).isEqualTo("login-token");
        assertThat(response.expiresIn()).isEqualTo(1800L);
        assertThat(response.user().id()).isEqualTo(7L);
        assertThat(response.user().username()).isEqualTo("johndoe");
        assertThat(response.user().fullName()).isEqualTo("John Doe");
        assertThat(response.user().roles()).containsExactly("CUSTOMER");
    }

    @Test
    void loginWithBlankLastNameReturnsFirstNameOnlyAsFullName() {
        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);
        AppUser user = AppUser.builder()
                .id(9L)
                .username("solo")
                .email("solo@example.com")
                .firstName("Solo")
                .lastName("  ")
                .active(true)
                .roles(roles)
                .build();

        when(appUserRepository.findByUsername("solo")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(SecurityUser.class))).thenReturn("solo-token");
        when(jwtService.getExpirationSeconds()).thenReturn(1800L);

        AuthResponse response = authService.login(new LoginRequest("solo", "Password123!"));

        assertThat(response.user().fullName()).isEqualTo("Solo");
    }

    @Test
    void loginWithBlankFirstNameReturnsLastNameOnlyAsFullName() {
        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);
        AppUser user = AppUser.builder()
                .id(10L)
                .username("nofirst")
                .email("nofirst@example.com")
                .firstName(null)
                .lastName("Onlylast")
                .active(true)
                .roles(roles)
                .build();

        when(appUserRepository.findByUsername("nofirst")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(SecurityUser.class))).thenReturn("nofirst-token");
        when(jwtService.getExpirationSeconds()).thenReturn(1800L);

        AuthResponse response = authService.login(new LoginRequest("nofirst", "Password123!"));

        assertThat(response.user().fullName()).isEqualTo("Onlylast");
    }
}
