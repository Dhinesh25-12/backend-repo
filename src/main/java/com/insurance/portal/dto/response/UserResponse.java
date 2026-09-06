package com.insurance.portal.dto.response;

import java.time.LocalDate;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        boolean active,
        List<String> roles,
        String address,
        String city,
        String state,
        String postalCode,
        LocalDate dateOfBirth,
        String kycIdType,
        String kycIdNumber
) {
}
