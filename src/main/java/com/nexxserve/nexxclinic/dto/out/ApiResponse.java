package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.ResponseStatus;

public record ApiResponse<T>(
        ResponseStatus status,
        String message,
        T data,
        Object pagination
) {

    public ApiResponse(ResponseStatus status, String message, T data) {
        this(status, message, data, null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                ResponseStatus.SUCCESS,
                message,
                data
        );
    }

    public static <T> ApiResponse<T> success(
            String message,
            T data,
            Object pagination
    ) {
        return new ApiResponse<>(
                ResponseStatus.SUCCESS,
                message,
                data,
                pagination
        );
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(
                ResponseStatus.ERROR,
                message,
                null
        );
    }

    public static ApiResponse<Void> unauthenticated(String message) {
        return new ApiResponse<>(
                ResponseStatus.UNAUTHENTICATED,
                message,
                null
        );
    }

    public static ApiResponse<Void> unauthorised(String message) {
        return new ApiResponse<>(
                ResponseStatus.UNAUTHORISED,
                message,
                null
        );
    }

    public static <T> ApiResponse<T> partialSuccess(
            String message,
            T data
    ) {
        return new ApiResponse<>(
                ResponseStatus.PARTIAL_SUCCESS,
                message,
                data
        );
    }
}