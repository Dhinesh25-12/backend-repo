package com.insurance.portal.dto.response;

public record PlatformSettingResponse(
        String key,
        String value,
        String description,
        boolean editable
) {
}
