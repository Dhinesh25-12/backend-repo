package com.insurance.portal.dto.response;

import java.time.LocalDate;

public record CustomerResponse(
        Long id,
        Long userId,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        boolean active,
        LocalDate dateOfBirth,
        String address,
        String city,
        String state,
        String postalCode,
        String kycIdType,
        String kycIdNumber,
        long policyCount
) {
}
