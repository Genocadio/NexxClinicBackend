package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.RoleName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record WorkerDto(
        UUID id,
        String firstName,
        String lastName,
        Gender gender,
        LocalDate dateOfBirth,
        String profilePhotoUrl,
        String email,
        String phoneNumber,
        String username,
        AccountStatus accountStatus,
        boolean active,
        Set<RoleName> roles,
        boolean autoReset,
        Integer resetPeriodDays,
        LocalDateTime lastPasswordChange,
        LocalDateTime nextResetDate,
        boolean mustChangeOnNextLogin,
        List<DepartmentDto> departments,
        DepartmentDto department,
        WorkerDocumentDto workerDocProfile, // Aligned with your entity's field
        Integer maxActiveSessions,
        Integer activeSessions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}