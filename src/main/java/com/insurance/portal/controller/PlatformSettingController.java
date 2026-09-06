package com.insurance.portal.controller;

import com.insurance.portal.dto.request.UpdatePlatformSettingRequest;
import com.insurance.portal.dto.response.PlatformSettingResponse;
import com.insurance.portal.service.PlatformSettingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Minimal admin-facing "Platform Settings" capability: a key/value store of platform
 * settings. Some settings are purely informational read-only copies of static app
 * config (e.g. CORS origins, JWT expiration); others are genuinely editable and take
 * effect immediately without a restart. See {@link PlatformSettingResponse#editable()}.
 */
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@Tag(name = "Platform Settings", description = "Admin platform settings configuration")
public class PlatformSettingController {

    private final PlatformSettingService platformSettingService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PlatformSettingResponse>> listSettings() {
        return ResponseEntity.ok(platformSettingService.listSettings());
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformSettingResponse> updateSetting(@PathVariable String key,
                                                                  @Valid @RequestBody UpdatePlatformSettingRequest request) {
        return ResponseEntity.ok(platformSettingService.updateSetting(key, request));
    }
}
