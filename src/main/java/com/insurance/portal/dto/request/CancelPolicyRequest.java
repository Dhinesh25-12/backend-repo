package com.insurance.portal.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelPolicyRequest(
        @NotBlank String reason
) {
}
