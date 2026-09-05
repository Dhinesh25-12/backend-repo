package com.insurance.portal.dto.request;

import com.insurance.portal.entity.RoleName;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull RoleName role
) {
}
