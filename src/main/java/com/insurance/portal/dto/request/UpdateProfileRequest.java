package com.insurance.portal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone,
        String address,
        String city,
        String state,
        String postalCode
) {
}
