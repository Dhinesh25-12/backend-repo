package com.insurance.portal.dto.request;

import com.insurance.portal.entity.ClaimStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateClaimStatusRequest(
        @NotNull ClaimStatus status,
        String remarks
) {
}
