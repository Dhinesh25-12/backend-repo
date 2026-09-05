package com.insurance.portal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PurchasePolicyRequest(
        @NotNull Long productId,
        @NotBlank String nomineeName,
        String nomineeRelationship,
        String nomineeContact
) {
}
