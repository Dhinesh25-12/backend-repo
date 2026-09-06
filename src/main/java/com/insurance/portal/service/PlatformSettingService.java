package com.insurance.portal.service;

import com.insurance.portal.dto.request.UpdatePlatformSettingRequest;
import com.insurance.portal.dto.response.PlatformSettingResponse;
import com.insurance.portal.entity.PlatformSetting;
import com.insurance.portal.exception.BadRequestException;
import com.insurance.portal.exception.ResourceNotFoundException;
import com.insurance.portal.repository.PlatformSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PlatformSettingService {

    public static final String POLICY_TENURE_OVERRIDE_KEY = "policies.default-tenure-months-override";

    private final PlatformSettingRepository platformSettingRepository;

    public List<PlatformSettingResponse> listSettings() {
        return platformSettingRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public PlatformSettingResponse updateSetting(String key, UpdatePlatformSettingRequest request) {
        PlatformSetting setting = findByKey(key);
        if (!setting.isEditable()) {
            throw new BadRequestException("Setting '" + key + "' is read-only/informational and cannot be modified");
        }
        setting.setValue(request.value());
        platformSettingRepository.save(setting);
        return toResponse(setting);
    }

    /**
     * Returns the currently configured policy tenure override (in months), if the
     * "{@value #POLICY_TENURE_OVERRIDE_KEY}" setting is present and set to a positive value.
     * Consumed directly by {@link PolicyService} so the effect is immediate, without a restart.
     */
    public Optional<Integer> getPolicyTenureMonthsOverride() {
        return platformSettingRepository.findByKey(POLICY_TENURE_OVERRIDE_KEY)
                .map(PlatformSetting::getValue)
                .flatMap(value -> {
                    try {
                        int months = Integer.parseInt(value.trim());
                        return months > 0 ? Optional.of(months) : Optional.<Integer>empty();
                    } catch (NumberFormatException | NullPointerException ex) {
                        return Optional.empty();
                    }
                });
    }

    private PlatformSetting findByKey(String key) {
        return platformSettingRepository.findByKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Platform setting not found: " + key));
    }

    private PlatformSettingResponse toResponse(PlatformSetting setting) {
        return new PlatformSettingResponse(setting.getKey(), setting.getValue(), setting.getDescription(),
                setting.isEditable());
    }
}
