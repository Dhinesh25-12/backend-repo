package com.insurance.portal.dto.response;

import java.util.List;

public record AuthResponse(
        String token,
        String tokenType,
        Long expiresIn,
        UserSummary user
) {
    public AuthResponse(String token, Long expiresIn, UserSummary user) {
        this(token, "Bearer", expiresIn, user);
    }

    public record UserSummary(
            Long id,
            String username,
            String email,
            String fullName,
            List<String> roles,
            boolean enabled
    ) {}
}
