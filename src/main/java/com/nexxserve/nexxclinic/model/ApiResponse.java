package com.nexxserve.nexxclinic.model;

public record ApiResponse(
        ResponseStatus status,
        String message,
        Object data,
        Object pagination
) {

    public ApiResponse(ResponseStatus status, String message, Object data) {
        this(status, message, data, null);
    }

    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(ResponseStatus.SUCCESS, message, data);
    }

    public static ApiResponse success(String message, Object data, Object pagination) {
        return new ApiResponse(ResponseStatus.SUCCESS, message, data, pagination);
    }

    public static ApiResponse error(String message, String code) {
        return new ApiResponse(ResponseStatus.ERROR, message, null);
    }

    public static ApiResponse unauthenticated(String message) {
        return new ApiResponse(ResponseStatus.UNAUTHENTICATED, message, null);
    }

    public static ApiResponse unauthorised(String message) {
        return new ApiResponse(ResponseStatus.UNAUTHORISED, message, null);
    }

    public static ApiResponse partialSuccess(String message, Object data) {
        return new ApiResponse(ResponseStatus.PARTIAL_SUCCESS, message, data);
    }
}
