package com.nexxserve.nexxclinic.config;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.hibernate.LazyInitializationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j                                 // ← adds a `log` field (slf4j)
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        /* ------------- USER-VISIBLE ERRORS ---------------- */
        if (ex instanceof ConstraintViolationException cve) {
            String message = cve.getConstraintViolations()
                    .stream()
                    .map(this::formatConstraintViolation)
                    .collect(Collectors.joining("; "));
            return buildError(env, ErrorType.BAD_REQUEST, "VALIDATION_ERROR", message);
        }

        if (ex instanceof BindException be) {
            String message = be.getBindingResult().getFieldErrors()
                    .stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            if (message.isBlank()) { message = "Input validation failed."; }
            return buildError(env, ErrorType.BAD_REQUEST, "VALIDATION_ERROR", message);
        }

        if (ex instanceof IllegalArgumentException iae) {
            return buildError(env, ErrorType.BAD_REQUEST, "VALIDATION_ERROR", iae.getMessage());
        }

        /* ------------- DATA / INTEGRITY ERRORS ---------------- */

        // A unique/FK/NOT-NULL constraint was violated at the DB level. These are
        // almost always fixable by the client (duplicate email, FK reference), so
        // surface them as a clean 400 instead of a 500. No raw constraint message
        // is leaked; the full detail goes to the server log.
        if (ex instanceof DataIntegrityViolationException dive) {
            log.warn("Data integrity violation on field '{}': {}",
                    env.getField().getName(), dive.getMostSpecificCause().getMessage());
            return buildError(env, ErrorType.BAD_REQUEST, "DATA_CONSTRAINT",
                    "The operation conflicts with existing data (duplicate value or a referenced record in use). "
                    + "Check the submitted values and try again.");
        }

        // A findByX returning Optional matched more than one row (legacy duplicate
        // data). Not the client's fault, but it must not surface as a raw 500 either.
        if (ex instanceof IncorrectResultSizeDataAccessException) {
            log.error("Multiple rows matched a unique lookup on field '{}'", env.getField().getName(), ex);
            return buildError(env, ErrorType.BAD_REQUEST, "DUPLICATE_DATA",
                    "Multiple records match the submitted identifier. Contact an administrator to reconcile the data.");
        }

        /* ------------- CONCURRENCY / LOCK ERRORS ---------------- */

        if (ex instanceof PessimisticLockingFailureException
                || ex instanceof OptimisticLockingFailureException) {
            log.warn("Concurrent update or lock contention on field '{}'", env.getField().getName(), ex);
            return buildError(env, ErrorType.BAD_REQUEST, "CONCURRENT_UPDATE",
                    "The record is being updated by another user. Please refresh and try again.");
        }        /* ------------- LAZY / PROXY ERRORS ---------------- */

        // LazyInitializationException fires when a @OneToMany / @ManyToOne collection
        // is accessed outside a transaction (e.g., a mapper called from a read-only
        // query that detached the entity). Surface as a recoverable error.
        if (ex instanceof LazyInitializationException lie) {
            log.warn("Lazy loading error on field '{}': {}", env.getField().getName(), lie.getMessage());
            return buildError(env, ErrorType.INTERNAL_ERROR, "LAZY_LOAD_ERROR",
                    "A related record could not be loaded. Please refresh and try again.");
        }

        /* ------------- NULL POINTER ERRORS ---------------- */

        // NullPointerException / IllegalArgument(=null check) from business logic
        // that hit an unexpected null. Log the context so the dev can trace it, but
        // surface a user-friendly message.
        if (ex instanceof NullPointerException npe) {
            log.error("NPE on field '{}': {}", env.getField().getName(), npe.getMessage(), npe);
            return buildError(env, ErrorType.INTERNAL_ERROR, "NULL_REFERENCE",
                    "A required record was missing. Please refresh and try again.");
        }

        /* ------------- INTERNAL ERRORS ------------------- */
        // Log the stack trace – *before* we build the error to keep it in your logs.
        log.error("GraphQL internal error on field '{}' ({}): {}",
                env.getField().getName(), ex.getClass().getSimpleName(),
                ex.getMessage(), ex);

        return buildError(env,
                ErrorType.INTERNAL_ERROR,
                "INTERNAL_ERROR",
                // Generic message: never leak stack traces / SQL / internal paths to the
                // frontend. The full detail is in the server log above.
                "An unexpected error occurred. Please try again or contact support.");
    }

    /* --------------------------------------------------------------------- */
    private String formatConstraintViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null
                ? "field"
                : violation.getPropertyPath().toString();
        return path + ": " + violation.getMessage();
    }

    private GraphQLError buildError(DataFetchingEnvironment env,
                                    ErrorType errorType,
                                    String code,
                                    String message) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(errorType)
                .message(message)
                .extensions(Map.of("code", code))
                .build();
    }
}
