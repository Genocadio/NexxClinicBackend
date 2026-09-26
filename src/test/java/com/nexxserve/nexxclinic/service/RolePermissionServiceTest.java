package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.RoleDto;
import com.nexxserve.nexxclinic.entity.Permission;
import com.nexxserve.nexxclinic.entity.Role;
import com.nexxserve.nexxclinic.graphql.input.AssignPermissionsToRoleInput;
import com.nexxserve.nexxclinic.graphql.input.CreateRoleInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateRoleInput;
import com.nexxserve.nexxclinic.model.PermissionName;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.repository.PermissionRepository;
import com.nexxserve.nexxclinic.repository.RoleRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RolePermissionServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    private RolePermissionService rolePermissionService;

    private AuthenticatedUser adminUser;
    private AuthenticatedUser clinicianUser;

    @BeforeEach
    void setUp() {
        rolePermissionService = new RolePermissionService(roleRepository, permissionRepository);

        adminUser = new AuthenticatedUser(
                UUID.randomUUID(),
                "admin@clinic.com",
                Set.of(RoleName.ADMIN),
                "token-admin",
                Instant.now().plusSeconds(3600)
        );

        clinicianUser = new AuthenticatedUser(
                UUID.randomUUID(),
                "doctor@clinic.com",
                Set.of(RoleName.CLINICIAN),
                "token-doc",
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void createRole_success() {
        CreateRoleInput input = new CreateRoleInput("PHARMACIST", "Dispenses medication", List.of("PRODUCT_VIEW", "MEDICATION_PRESCRIBE"));
        Permission p1 = new Permission("PRODUCT_VIEW", "View products", "PRODUCT");
        Permission p2 = new Permission("MEDICATION_PRESCRIBE", "Prescribe meds", "CONSULTATION");

        when(roleRepository.existsByName("PHARMACIST")).thenReturn(false);
        when(permissionRepository.findByNameIn(anyCollection())).thenReturn(List.of(p1, p2));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ApiResponse response = rolePermissionService.createRole(input, adminUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());
        RoleDto dto = (RoleDto) response.data();
        assertEquals("PHARMACIST", dto.name());
        assertEquals(2, dto.permissions().size());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createRole_rejectsDuplicateName() {
        CreateRoleInput input = new CreateRoleInput("ADMIN", "Duplicate admin", List.of());
        when(roleRepository.existsByName("ADMIN")).thenReturn(true);

        ApiResponse response = rolePermissionService.createRole(input, adminUser);

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("already exists"));
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void deleteRole_rejectsSystemRole() {
        UUID roleId = UUID.randomUUID();
        Role systemRole = new Role("ADMIN", "Superadmin", true);
        systemRole.setId(roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(systemRole));

        ApiResponse response = rolePermissionService.deleteRole(roleId, adminUser);

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("System roles cannot be deleted"));
        verify(roleRepository, never()).delete(any(Role.class));
    }

    @Test
    void deleteRole_successForCustomRole() {
        UUID roleId = UUID.randomUUID();
        Role customRole = new Role("INTERN", "Intern doctor", false);
        customRole.setId(roleId);

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(customRole));

        ApiResponse response = rolePermissionService.deleteRole(roleId, adminUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        verify(roleRepository).delete(customRole);
    }

    @Test
    void assignPermissionsToRole_success() {
        UUID roleId = UUID.randomUUID();
        Role role = new Role("LAB_TECH", "Lab Technician", false);
        role.setId(roleId);

        Permission p = new Permission("PRODUCT_VIEW", "View products", "PRODUCT");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findByNameIn(anyCollection())).thenReturn(List.of(p));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignPermissionsToRoleInput input = new AssignPermissionsToRoleInput(roleId, List.of("PRODUCT_VIEW"));
        ApiResponse response = rolePermissionService.assignPermissionsToRole(input, adminUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertEquals(1, role.getPermissions().size());
        verify(roleRepository).save(role);
    }

    @Test
    void userHasAnyPermission_adminAlwaysHasPermission() {
        assertTrue(rolePermissionService.userHasAnyPermission(adminUser, PermissionName.BILLING_PROCESS));
        assertTrue(rolePermissionService.userHasAnyPermission(adminUser, PermissionName.USER_CREATE));
    }

    @Test
    void userHasAnyPermission_evaluatesRolePermissions() {
        Role clinicianRole = new Role("CLINICIAN", "Doctor", true);
        Permission p = new Permission("CONSULTATION_CONDUCT", "Conduct consult", "CONSULTATION");
        clinicianRole.setPermissions(new LinkedHashSet<>(Set.of(p)));

        when(roleRepository.findByName("CLINICIAN")).thenReturn(Optional.of(clinicianRole));

        assertTrue(rolePermissionService.userHasAnyPermission(clinicianUser, PermissionName.CONSULTATION_CONDUCT));
        assertFalse(rolePermissionService.userHasAnyPermission(clinicianUser, PermissionName.BILLING_PROCESS));
    }
}
