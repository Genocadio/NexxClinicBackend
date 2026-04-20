package com.nexxserve.nexxclinic.model;

import java.util.List;

public record ApiResponse(
        ResponseStatus status,
        String message,
        List<ErrorDetail> errors,
        List<String> errorMessages,
        Object data,
        Object pagination
) {

    public ApiResponse(ResponseStatus status, String message, List<ErrorDetail> errors, Object data) {
        this(status, message, errors, extractErrorMessages(errors), data, null);
    }

    public ApiResponse(ResponseStatus status, String message, List<ErrorDetail> errors, Object data, Object pagination) {
        this(status, message, errors, extractErrorMessages(errors), data, pagination);
    }

    private static List<String> extractErrorMessages(List<ErrorDetail> errors) {
        if (errors == null || errors.isEmpty()) {
            return List.of();
        }
        return errors.stream().map(ErrorDetail::message).toList();
    }

    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(ResponseStatus.SUCCESS, message, List.of(), data);
    }

    public static ApiResponse success(String message, Object data, Object pagination) {
        return new ApiResponse(ResponseStatus.SUCCESS, message, List.of(), data, pagination);
    }

    public static ApiResponse error(String message, String code) {
        return new ApiResponse(
                ResponseStatus.ERROR,
                message,
                List.of(new ErrorDetail(null, message, code)),
                null
        );
    }

    public static ApiResponse unauthenticated(String message) {
        return new ApiResponse(
                ResponseStatus.UNAUTHENTICATED,
                message,
                List.of(new ErrorDetail(null, message, "UNAUTHENTICATED")),
                null
        );
    }

    public static ApiResponse unauthorised(String message) {
        return new ApiResponse(
                ResponseStatus.UNAUTHORISED,
                message,
                List.of(new ErrorDetail(null, message, "UNAUTHORISED")),
                null
        );
    }
}
