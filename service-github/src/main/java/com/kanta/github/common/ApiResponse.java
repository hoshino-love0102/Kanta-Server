package com.kanta.github.common;

public record ApiResponse<T>(
    int status,
    String message,
    T data,
    String code
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "OK", data, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "Created", data, null);
    }

    public static <T> ApiResponse<T> accepted(T data) {
        return new ApiResponse<>(202, "Accepted", data, null);
    }
}
