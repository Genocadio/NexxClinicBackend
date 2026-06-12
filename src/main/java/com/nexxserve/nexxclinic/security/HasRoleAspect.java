package com.nexxserve.nexxclinic.security;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
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

    @Around("@annotation(hasRole)")
    public Object enforceRole(ProceedingJoinPoint joinPoint, HasRole hasRole) throws Throwable {
        AuthenticatedUser authUser = findAuthUser(joinPoint.getArgs());
        
        if (authUser == null) {
            logger.warn("Access denied: Authentication is required for method: {}", joinPoint.getSignature().getName());
            return ApiResponse.unauthenticated("Authentication is required for this operation.");
        }

        Set<RoleName> userRoles = authUser.roles();
        boolean authorised = Arrays.stream(hasRole.value()).anyMatch(userRoles::contains);
        
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

    private AuthenticatedUser findAuthUser(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof AuthenticatedUser authenticatedUser) {
                return authenticatedUser;
            }
        }
        return null;
    }
}
