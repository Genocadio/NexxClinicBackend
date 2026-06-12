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

@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ConstraintViolationException constraintViolationException) {
            String message = constraintViolationException.getConstraintViolations()
                    .stream()
                    .map(this::formatConstraintViolation)
                    .collect(Collectors.joining("; "));
            return buildError(env, ErrorType.BAD_REQUEST, "VALIDATION_ERROR", message);
        }

        if (ex instanceof BindException bindException) {
            String message = bindException.getBindingResult()
                    .getFieldErrors()
                    .stream()
                    .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                    .collect(Collectors.joining("; "));
            if (message.isBlank()) {
                message = "Input validation failed.";
            }
            return buildError(env, ErrorType.BAD_REQUEST, "VALIDATION_ERROR", message);
        }

        if (ex instanceof IllegalArgumentException) {
            return buildError(env, ErrorType.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage());
        }

        return buildError(env, ErrorType.INTERNAL_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private String formatConstraintViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "field" : violation.getPropertyPath().toString();
        return path + ": " + violation.getMessage();
    }

    private GraphQLError buildError(DataFetchingEnvironment env, ErrorType errorType, String code, String message) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(errorType)
                .message(message)
                .extensions(Map.of("code", code))
                .build();
    }
}
