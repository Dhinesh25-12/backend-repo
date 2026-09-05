package com.insurance.portal.dto.response;

import java.util.List;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        List<String> roles
) {
    public AuthResponse(String token, Long userId, String username, List<String> roles) {
        this(token, "Bearer", userId, username, roles);
    }
}
