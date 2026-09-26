package com.nexxserve.nexxclinic.config;

import com.nexxserve.nexxclinic.entity.Permission;
import com.nexxserve.nexxclinic.entity.Role;
import com.nexxserve.nexxclinic.model.PermissionName;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.repository.PermissionRepository;
import com.nexxserve.nexxclinic.repository.RoleRepository;
import com.nexxserve.nexxclinic.service.RolePermissionService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RolePermissionInitializer {

    private static final Logger logger = LoggerFactory.getLogger(RolePermissionInitializer.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionService rolePermissionService;

    public RolePermissionInitializer(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            RolePermissionService rolePermissionService
    ) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionService = rolePermissionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeRolesAndPermissions() {
        logger.info("Verifying system roles and permissions in database...");

        // 1. Ensure all PermissionName enum values exist in DB
        for (PermissionName permEnum : PermissionName.values()) {
            if (!permissionRepository.existsByName(permEnum.name())) {
                String module = resolveModule(permEnum);
                String desc = formatDescription(permEnum.name());
                permissionRepository.save(new Permission(permEnum.name(), desc, module));
                logger.info("Registered missing permission in DB: {}", permEnum.name());
            }
        }

        // 2. Ensure all default system roles exist in DB
        for (RoleName roleEnum : RoleName.values()) {
            if (!roleRepository.existsByName(roleEnum.name())) {
                Role role = new Role(roleEnum.name(), formatDescription(roleEnum.name()) + " Role", true);
                roleRepository.save(role);
                logger.info("Registered missing system role in DB: {}", roleEnum.name());
            }
        }

        // 3. Ensure ADMIN role has all permissions
        Optional<Role> adminRoleOpt = roleRepository.findByName(RoleName.ADMIN.name());
        if (adminRoleOpt.isPresent()) {
            Role adminRole = adminRoleOpt.get();
            List<Permission> allPerms = permissionRepository.findAll();
            if (adminRole.getPermissions().size() < allPerms.size()) {
                adminRole.setPermissions(new LinkedHashSet<>(allPerms));
                roleRepository.save(adminRole);
                logger.info("Updated ADMIN role with full permissions set ({})", allPerms.size());
            }
        }

        rolePermissionService.invalidateCache();
        logger.info("Roles and permissions verification completed successfully.");
    }

    private String resolveModule(PermissionName perm) {
        String name = perm.name();
        if (name.startsWith("USER_") || name.startsWith("ROLE_")) return "USER";
        if (name.startsWith("CLINIC_")) return "CLINIC";
        if (name.startsWith("DEPARTMENT_")) return "DEPARTMENT";
        if (name.startsWith("FORM_TEMPLATE_")) return "FORM_TEMPLATE";
        if (name.startsWith("PATIENT_")) return "PATIENT";
        if (name.startsWith("VISIT_")) return "VISIT";
        if (name.startsWith("TRIAGE_") || name.startsWith("VITAL_")) return "TRIAGE";
        if (name.startsWith("CONSULTATION_") || name.startsWith("DIAGNOSIS_") || name.startsWith("MEDICATION_") || name.startsWith("CLINICAL_")) return "CONSULTATION";
        if (name.startsWith("BILLING_") || name.startsWith("INVOICE_")) return "BILLING";
        if (name.startsWith("PRODUCT_") || name.startsWith("INSURANCE_")) return "PRODUCT";
        return "ADMIN";
    }

    private String formatDescription(String name) {
        return name.replace("_", " ").toLowerCase();
    }
}
