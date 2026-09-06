package com.insurance.portal.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-key-for-jwt-signing-in-unit-tests-1234567890", 3600000L);
    }

    @Test
    void generatesTokenAndExtractsUsername() {
        SecurityUser user = new SecurityUser(com.insurance.portal.entity.AppUser.builder()
                .id(1L)
                .username("john")
                .passwordHash("hash")
                .active(true)
                .build());

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("john");
        assertThat(jwtService.isTokenValid(token, "john")).isTrue();
        assertThat(jwtService.isTokenValid(token, "someone-else")).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = new JwtService("test-secret-key-for-jwt-signing-in-unit-tests-1234567890", -1000L);
        SecurityUser user = new SecurityUser(com.insurance.portal.entity.AppUser.builder()
                .id(2L)
                .username("expired-user")
                .passwordHash("hash")
                .active(true)
                .build());

        String token = shortLived.generateToken(user);

        org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> shortLived.isTokenValid(token, "expired-user"));
    }
}
