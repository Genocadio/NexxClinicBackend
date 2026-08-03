package com.nexxserve.nexxclinic.config;

import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ResultPath;
import graphql.language.Field;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the {@link GraphQlExceptionResolver} error contract:
 * every unexpected exception must become a clean, non-leaking GraphQL error
 * instead of a raw 500 with the internal message surfaced to the client.
 */
class GraphQlExceptionResolverTest {

    private final GraphQlExceptionResolver resolver = new GraphQlExceptionResolver();

    /**
     * Builds a minimally-usable {@link DataFetchingEnvironment} (a field with a
     * source location and a root execution path) so the resolver's error builder
     * does not NPE while computing location/path metadata.
     */
    private DataFetchingEnvironment env() {
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        Field field = mock(Field.class);
        when(field.getName()).thenReturn("testField");
        when(field.getSourceLocation()).thenReturn(new SourceLocation(1, 1));
        when(env.getField()).thenReturn(field);
        ExecutionStepInfo stepInfo = mock(ExecutionStepInfo.class);
        when(stepInfo.getPath()).thenReturn(ResultPath.rootPath());
        when(env.getExecutionStepInfo()).thenReturn(stepInfo);
        return env;
    }

    private GraphQLError resolveSingle(Throwable ex) {
        // resolveException is reactive (Mono); block to inspect the resolved error.
        List<GraphQLError> errors = resolver.resolveException(ex, env()).block();
        assertNotNull(errors, "resolver must always produce an error for a thrown exception");
        assertEquals(1, errors.size());
        return errors.get(0);
    }

    @Test
    void genericRuntimeExceptionMapsToInternalErrorWithoutLeakingMessage() {
        GraphQLError error = resolveSingle(
                new RuntimeException("secret-internal-detail-xyz")
        );

        assertEquals("INTERNAL_ERROR", error.getExtensions().get("code"));
        assertEquals(
                "An unexpected error occurred. Please try again or contact support.",
                error.getMessage()
        );
        assertFalse(
                error.getMessage().contains("secret-internal-detail-xyz"),
                "internal exception detail must never leak to the client"
        );
    }

    @Test
    void dataIntegrityViolationMapsToDataConstraintWithoutLeakingConstraintName() {
        GraphQLError error = resolveSingle(
                new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"workers_email_key\""
                )
        );

        assertEquals("DATA_CONSTRAINT", error.getExtensions().get("code"));
        assertTrue(
                error.getMessage().contains("conflicts with existing data"),
                "message should explain the conflict in user terms"
        );
        assertFalse(
                error.getMessage().contains("workers_email_key"),
                "raw constraint names must never leak to the client"
        );
    }

    @Test
    void lockFailuresMapToConcurrentUpdate() {
        GraphQLError pessimistic = resolveSingle(
                new PessimisticLockingFailureException("could not acquire lock")
        );
        assertEquals("CONCURRENT_UPDATE", pessimistic.getExtensions().get("code"));
        assertTrue(pessimistic.getMessage().contains("being updated by another user"));

        GraphQLError optimistic = resolveSingle(
                new OptimisticLockingFailureException("version mismatch")
        );
        assertEquals("CONCURRENT_UPDATE", optimistic.getExtensions().get("code"));
    }

    @Test
    void duplicateOptionalLookupMapsToDuplicateData() {
        GraphQLError error = resolveSingle(
                new IncorrectResultSizeDataAccessException(1, 2)
        );

        assertEquals("DUPLICATE_DATA", error.getExtensions().get("code"));
        assertTrue(error.getMessage().contains("reconcile the data"));
    }

    @Test
    void illegalArgumentMapsToValidationErrorKeepingItsMessage() {
        GraphQLError error = resolveSingle(
                new IllegalArgumentException("quantity must be greater than 0")
        );

        assertEquals("VALIDATION_ERROR", error.getExtensions().get("code"));
        assertTrue(
                error.getMessage().contains("quantity must be greater than 0"),
                "IllegalArgumentException messages are safe to surface as validation errors"
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void beanConstraintViolationMapsToValidationErrorListingFields() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("firstName");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        GraphQLError error = resolveSingle(
                new ConstraintViolationException(Set.of(violation))
        );

        assertEquals("VALIDATION_ERROR", error.getExtensions().get("code"));
        assertTrue(error.getMessage().contains("firstName"));
        assertTrue(error.getMessage().contains("must not be blank"));
    }
}
