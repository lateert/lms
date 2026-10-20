package com.lms.shared;

public class AppException extends RuntimeException {
    private final String code;

    public AppException(String message, String code) {
        super(message);
        this.code = code;
    }

    public static AppException notFound(String message) {
        return new AppException(message, "NOT_FOUND");
    }

    public static AppException unauthorized(String message) {
        return new AppException(message, "UNAUTHORIZED");
    }

    public static AppException forbidden(String message) {
        return new AppException(message, "FORBIDDEN");
    }

    public String getCode() {
        return code;
    }
}
