package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.UserSetting;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.UpdateUserSettingsInput;
import com.nexxserve.nexxclinic.repository.UserSettingRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingService {

    private static final Logger logger = LoggerFactory.getLogger(UserSettingService.class);

    private final UserSettingRepository userSettingRepository;
    private final WorkerRepository workerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserSettingService(
            UserSettingRepository userSettingRepository,
            WorkerRepository workerRepository
    ) {
        this.userSettingRepository = userSettingRepository;
        this.workerRepository = workerRepository;
    }

    @Transactional(readOnly = true)
    public ApiResponse getMySettings(UUID workerId) {
        if (workerId == null) {
            return ApiResponse.unauthenticated("User is not authenticated");
        }
        Optional<UserSetting> settingOpt = userSettingRepository.findByWorkerId(workerId);
        if (settingOpt.isEmpty()) {
            // Return empty / default settings map
            Map<String, Object> defaults = new HashMap<>();
            defaults.put("id", null);
            defaults.put("fontSize", null);
            defaults.put("theme", "system");
            defaults.put("settingsJson", null);
            return ApiResponse.success("User settings fetched", defaults);
        }
        return ApiResponse.success("User settings fetched", settingToMap(settingOpt.get()));
    }

    @Transactional
    public ApiResponse updateMySettings(UUID workerId, UpdateUserSettingsInput input) {
        if (workerId == null) {
            return ApiResponse.unauthenticated("User is not authenticated");
        }

        Optional<Worker> workerOpt = workerRepository.findById(workerId);
        if (workerOpt.isEmpty()) {
            return ApiResponse.error("User not found");
        }
        Worker worker = workerOpt.get();

        UserSetting setting = userSettingRepository.findByWorkerId(workerId).orElseGet(() -> {
            UserSetting s = new UserSetting();
            s.setWorker(worker);
            s.setTheme("system");
            return s;
        });

        if (input.fontSize() != null && !input.fontSize().isBlank()) {
            setting.setFontSize(input.fontSize().trim().toLowerCase());
        }

        if (input.theme() != null && !input.theme().isBlank()) {
            setting.setTheme(input.theme().trim().toLowerCase());
        }

        if (input.settingsJson() != null) {
            try {
                if (input.settingsJson() instanceof String str) {
                    setting.setSettingsJson(str);
                } else {
                    setting.setSettingsJson(objectMapper.writeValueAsString(input.settingsJson()));
                }
            } catch (JsonProcessingException e) {
                logger.warn("Could not serialize settingsJson: {}", e.getMessage());
            }
        }

        UserSetting saved = userSettingRepository.save(setting);
        return ApiResponse.success("User settings updated", settingToMap(saved));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSettingsMap(UUID workerId) {
        if (workerId == null) return null;
        return userSettingRepository.findByWorkerId(workerId)
                .map(this::settingToMap)
                .orElse(null);
    }

    public Map<String, Object> settingToMap(UserSetting setting) {
        if (setting == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", setting.getId());
        map.put("workerId", setting.getWorker() != null ? setting.getWorker().getId() : null);
        map.put("fontSize", setting.getFontSize());
        map.put("theme", setting.getTheme() != null ? setting.getTheme() : "system");
        
        Object parsedJson = null;
        if (setting.getSettingsJson() != null && !setting.getSettingsJson().isBlank()) {
            try {
                parsedJson = objectMapper.readValue(setting.getSettingsJson(), Object.class);
            } catch (Exception e) {
                parsedJson = setting.getSettingsJson();
            }
        }
        map.put("settingsJson", parsedJson);
        map.put("createdAt", setting.getCreatedAt());
        map.put("updatedAt", setting.getUpdatedAt());
        return map;
    }
}
