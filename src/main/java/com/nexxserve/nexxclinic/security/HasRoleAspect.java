package com.nexxserve.nexxclinic.security;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.service.RolePermissionService;
import java.util.Arrays;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class HasRoleAspect {

    private static final Logger logger = LoggerFactory.getLogger(HasRoleAspect.class);

    private final RolePermissionService rolePermissionService;

    public HasRoleAspect(RolePermissionService rolePermissionService) {
        this.rolePermissionService = rolePermissionService;
    }

    @Around("@annotation(hasRole)")
    public Object enforceRole(ProceedingJoinPoint joinPoint, HasRole hasRole) throws Throwable {
        AuthenticatedUser authUser = findAuthUser(joinPoint.getArgs());
        
        if (authUser == null) {
            logger.warn("Access denied: Authentication is required for method: {}", joinPoint.getSignature().getName());
            return ApiResponse.unauthenticated("Authentication is required for this operation.");
        }

        Set<RoleName> userRoles = authUser.roles();
        boolean authorised = userRoles != null && (
                userRoles.contains(RoleName.ADMIN) ||
                Arrays.stream(hasRole.value()).anyMatch(userRoles::contains)
        );
        
        if (!authorised) {
            logger.warn("Access denied: User {} does not have required roles {}. User has: {}", 
                authUser.principal(), 
                Arrays.toString(hasRole.value()), 
                userRoles);
            return ApiResponse.unauthorised("You do not have permission to perform this operation.");
        }

        logger.debug("Access granted: User {} called method {} with roles: {}", 
            authUser.principal(), 
            joinPoint.getSignature().getName(), 
            userRoles);
        
        return joinPoint.proceed();
    }

    @Around("@annotation(hasPermission)")
    public Object enforcePermission(ProceedingJoinPoint joinPoint, HasPermission hasPermission) throws Throwable {
        AuthenticatedUser authUser = findAuthUser(joinPoint.getArgs());
        
        if (authUser == null) {
            logger.warn("Access denied: Authentication is required for method: {}", joinPoint.getSignature().getName());
            return ApiResponse.unauthenticated("Authentication is required for this operation.");
        }

        boolean authorised = rolePermissionService.userHasAnyPermission(authUser, hasPermission.value());
        
        if (!authorised) {
            logger.warn("Access denied: User {} lacks required permissions {}. Roles: {}", 
                authUser.principal(), 
                Arrays.toString(hasPermission.value()), 
                authUser.roles());
            return ApiResponse.unauthorised("You do not have permission to perform this operation.");
        }

        logger.debug("Access granted: User {} called method {} with permissions: {}", 
            authUser.principal(), 
            joinPoint.getSignature().getName(), 
            Arrays.toString(hasPermission.value()));
        
        return joinPoint.proceed();
    }

    private AuthenticatedUser findAuthUser(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof AuthenticatedUser authenticatedUser) {
                return authenticatedUser;
            }
        }
        return null;
    }
}
