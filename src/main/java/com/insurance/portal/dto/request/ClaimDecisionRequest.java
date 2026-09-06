package com.insurance.portal.dto.request;

import com.insurance.portal.entity.ClaimStatus;
import jakarta.validation.constraints.NotNull;

public record ClaimDecisionRequest(
        @NotNull ClaimStatus status,
        String decisionNotes
) {
}
