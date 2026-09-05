package com.insurance.portal.dto.response;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        boolean active,
        List<String> roles
) {
}
