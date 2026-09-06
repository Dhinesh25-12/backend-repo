package com.insurance.portal.dto.request;

import com.insurance.portal.entity.RoleName;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateUserRolesRequest(
        @NotEmpty List<RoleName> roles
) {
}
