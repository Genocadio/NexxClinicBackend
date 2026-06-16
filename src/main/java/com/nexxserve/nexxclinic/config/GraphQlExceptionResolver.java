package com.nexxserve.nexxclinic.config;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.stream.Collectors;
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
        /* ------------- USER‑VISIBLE ERRORS ---------------- */
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

        /* ------------- INTERNAL ERRORS ------------------- */
        // Log the stack trace – *before* we build the error to keep it in your logs.
        log.error("GraphQL internal error on field '{}' ({}): {}",
                env.getField().getName(),
                ex.getClass().getSimpleName(),
                ex.getMessage(), ex);

        return buildError(env,
                ErrorType.INTERNAL_ERROR,
                "INTERNAL_ERROR",
                // give the client something a little more useful than “An unexpected error occurred.”
                ex.getMessage() != null ? ex.getMessage()
                        : "An internal server error occurred.");
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
