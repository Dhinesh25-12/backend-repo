package com.insurance.portal.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull Boolean enabled
) {
}
