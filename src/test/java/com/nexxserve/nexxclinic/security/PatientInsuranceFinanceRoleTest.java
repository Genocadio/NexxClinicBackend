package com.nexxserve.nexxclinic.security;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.PatientInsuranceMutationController;
import com.nexxserve.nexxclinic.graphql.input.UpdatePatientInsuranceInput;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.service.RolePermissionService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientInsuranceFinanceRoleTest {

    private final HasRoleAspect aspect = new HasRoleAspect(mock(RolePermissionService.class));
    private final AuthenticatedUser financeUser = new AuthenticatedUser(
            UUID.randomUUID(),
            "finance-user",
            Set.of(RoleName.FINANCE),
            "test-token",
            Instant.now().plusSeconds(60)
    );

    @Test
    void financeCanUpdatePatientInsurance() throws Throwable {
        assertFinanceCanInvoke(
                "updatePatientInsurance",
                UUID.class,
                UpdatePatientInsuranceInput.class,
                AuthenticatedUser.class
        );
    }

    @Test
    void financeCanDeletePatientInsurance() throws Throwable {
        assertFinanceCanInvoke("deletePatientInsurance", UUID.class, AuthenticatedUser.class);
    }

    private void assertFinanceCanInvoke(String methodName, Class<?>... parameterTypes)
            throws Throwable {
        var method = java.util.Arrays.stream(
                        PatientInsuranceMutationController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .filter(candidate -> java.util.Arrays.equals(
                        candidate.getParameterTypes(), parameterTypes))
                .findFirst()
                .orElseThrow();
        HasRole roles = method.getAnnotation(HasRole.class);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);

        when(joinPoint.getArgs()).thenReturn(new Object[] { financeUser });
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("authorized");

        assertEquals("authorized", aspect.enforceRole(joinPoint, roles));
    }
}
