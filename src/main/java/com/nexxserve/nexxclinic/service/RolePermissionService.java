package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.PermissionDto;
import com.nexxserve.nexxclinic.dto.out.RoleDto;
import com.nexxserve.nexxclinic.entity.Permission;
import com.nexxserve.nexxclinic.entity.Role;
import com.nexxserve.nexxclinic.graphql.input.AssignPermissionsToRoleInput;
import com.nexxserve.nexxclinic.graphql.input.CreateRoleInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateRoleInput;
import com.nexxserve.nexxclinic.model.PermissionName;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.repository.PermissionRepository;
import com.nexxserve.nexxclinic.repository.RoleRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolePermissionService {

    private static final Logger logger = LoggerFactory.getLogger(RolePermissionService.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    // Cache: RoleName (upper case) -> Set of Permission Names (upper case)
    private final Map<String, Set<String>> rolePermissionsCache = new ConcurrentHashMap<>();

    public RolePermissionService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapRoleToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<RoleDto> getRoleById(UUID id) {
        return roleRepository.findById(id).map(this::mapRoleToDto);
    }

    @Transactional(readOnly = true)
    public Optional<RoleDto> getRoleByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return roleRepository.findByName(name.trim().toUpperCase()).map(this::mapRoleToDto);
    }

    @Transactional(readOnly = true)
    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapPermissionToDto)
                .toList();
    }

    @Transactional
    public ApiResponse createRole(CreateRoleInput input, AuthenticatedUser authUser) {
        if (input == null || input.name() == null || input.name().isBlank()) {
            return ApiResponse.error("Role name is required.");
        }

        String normalizedName = input.name().trim().toUpperCase();
        if (roleRepository.existsByName(normalizedName)) {
            return ApiResponse.error("A role with name '" + normalizedName + "' already exists.");
        }

        Role role = new Role(normalizedName, input.description(), false);

        if (input.permissionNames() != null && !input.permissionNames().isEmpty()) {
            Set<String> requestedPerms = input.permissionNames().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            List<Permission> matchedPermissions = permissionRepository.findByNameIn(requestedPerms);
            role.setPermissions(new LinkedHashSet<>(matchedPermissions));
        }

        Role saved = roleRepository.save(role);
        invalidateCache();
        logger.info("Created new custom role: {} by user: {}", saved.getName(), authUser != null ? authUser.principal() : "system");

        return ApiResponse.success("Role created successfully.", mapRoleToDto(saved));
    }

    @Transactional
    public ApiResponse updateRole(UpdateRoleInput input, AuthenticatedUser authUser) {
        if (input == null || input.id() == null) {
            return ApiResponse.error("Role ID is required.");
        }

        Optional<Role> roleOpt = roleRepository.findById(input.id());
        if (roleOpt.isEmpty()) {
            return ApiResponse.error("Role not found.");
        }

        Role role = roleOpt.get();

        if (input.name() != null && !input.name().isBlank()) {
            String newNormalizedName = input.name().trim().toUpperCase();
            if (!role.getName().equalsIgnoreCase(newNormalizedName)) {
                if (role.isSystem()) {
                    return ApiResponse.error("System roles cannot be renamed.");
                }
                if (roleRepository.existsByName(newNormalizedName)) {
                    return ApiResponse.error("A role with name '" + newNormalizedName + "' already exists.");
                }
                role.setName(newNormalizedName);
            }
        }

        if (input.description() != null) {
            role.setDescription(input.description());
        }

        if (input.permissionNames() != null) {
            Set<String> requestedPerms = input.permissionNames().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            List<Permission> matchedPermissions = permissionRepository.findByNameIn(requestedPerms);
            role.setPermissions(new LinkedHashSet<>(matchedPermissions));
        }

        Role saved = roleRepository.save(role);
        invalidateCache();
        logger.info("Updated role: {} by user: {}", saved.getName(), authUser != null ? authUser.principal() : "system");

        return ApiResponse.success("Role updated successfully.", mapRoleToDto(saved));
    }

    @Transactional
    public ApiResponse deleteRole(UUID roleId, AuthenticatedUser authUser) {
        if (roleId == null) {
            return ApiResponse.error("Role ID is required.");
        }

        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            return ApiResponse.error("Role not found.");
        }

        Role role = roleOpt.get();
        if (role.isSystem()) {
            return ApiResponse.error("System roles cannot be deleted.");
        }

        roleRepository.delete(role);
        invalidateCache();
        logger.info("Deleted role: {} by user: {}", role.getName(), authUser != null ? authUser.principal() : "system");

        return ApiResponse.success("Role deleted successfully.", true);
    }

    @Transactional
    public ApiResponse assignPermissionsToRole(AssignPermissionsToRoleInput input, AuthenticatedUser authUser) {
        if (input == null || input.roleId() == null) {
            return ApiResponse.error("Role ID is required.");
        }

        Optional<Role> roleOpt = roleRepository.findById(input.roleId());
        if (roleOpt.isEmpty()) {
            return ApiResponse.error("Role not found.");
        }

        Role role = roleOpt.get();

        Set<String> requestedPerms = (input.permissionNames() != null ? input.permissionNames() : List.<String>of())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        List<Permission> matchedPermissions = permissionRepository.findByNameIn(requestedPerms);
        role.setPermissions(new LinkedHashSet<>(matchedPermissions));

        Role saved = roleRepository.save(role);
        invalidateCache();
        logger.info("Updated permissions for role: {} (count: {}) by user: {}",
                saved.getName(), matchedPermissions.size(), authUser != null ? authUser.principal() : "system");

        return ApiResponse.success("Permissions assigned successfully.", mapRoleToDto(saved));
    }

    /**
     * Resolves all permissions granted to a collection of role names.
     */
    public Set<String> getPermissionsForRoleNames(Collection<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> effectivePermissions = new HashSet<>();
        for (String rawRole : roleNames) {
            if (rawRole == null || rawRole.isBlank()) continue;
            String normalizedRole = rawRole.trim().toUpperCase();

            Set<String> cached = rolePermissionsCache.get(normalizedRole);
            if (cached != null) {
                effectivePermissions.addAll(cached);
            } else {
                Set<String> loaded = loadAndCacheRolePermissions(normalizedRole);
                effectivePermissions.addAll(loaded);
            }
        }

        return effectivePermissions;
    }

    /**
     * Check if a user has any of the specified permissions.
     */
    public boolean userHasAnyPermission(AuthenticatedUser user, PermissionName... requiredPermissions) {
        if (user == null || requiredPermissions == null || requiredPermissions.length == 0) {
            return false;
        }

        // ADMIN role is super-admin and has all permissions
        Set<RoleName> userRoles = user.roles();
        if (userRoles != null && userRoles.contains(RoleName.ADMIN)) {
            return true;
        }

        Set<String> roleNames = userRoles != null
                ? userRoles.stream().map(Enum::name).collect(Collectors.toSet())
                : Collections.emptySet();

        Set<String> userPermissions = getPermissionsForRoleNames(roleNames);

        for (PermissionName perm : requiredPermissions) {
            if (perm != null && userPermissions.contains(perm.name())) {
                return true;
            }
        }

        return false;
    }

    private synchronized Set<String> loadAndCacheRolePermissions(String roleName) {
        // Double check cache under lock
        Set<String> existing = rolePermissionsCache.get(roleName);
        if (existing != null) {
            return existing;
        }

        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        if (roleOpt.isEmpty()) {
            rolePermissionsCache.put(roleName, Collections.emptySet());
            return Collections.emptySet();
        }

        Set<String> perms = roleOpt.get().getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toUnmodifiableSet());

        rolePermissionsCache.put(roleName, perms);
        return perms;
    }

    public void invalidateCache() {
        rolePermissionsCache.clear();
        logger.debug("Cleared role permissions cache.");
    }

    public RoleDto mapRoleToDto(Role role) {
        if (role == null) return null;
        List<PermissionDto> permissionDtos = role.getPermissions() != null
                ? role.getPermissions().stream().map(this::mapPermissionToDto).toList()
                : List.of();

        return new RoleDto(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                permissionDtos,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    public PermissionDto mapPermissionToDto(Permission permission) {
        if (permission == null) return null;
        return new PermissionDto(
                permission.getId(),
                permission.getName(),
                permission.getDescription(),
                permission.getModule()
        );
    }
}
