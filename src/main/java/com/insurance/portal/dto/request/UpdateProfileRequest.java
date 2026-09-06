package com.insurance.portal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        String address,
        String city,
        String state,
        String postalCode,
        @PastOrPresent(message = "Date of birth cannot be in the future") LocalDate dateOfBirth,
        @Size(max = 50) String kycIdType,
        @Size(max = 100) String kycIdNumber
) {
}
