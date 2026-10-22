package com.lms.shared;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class Times {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Times() {
    }

    public static String now() {
        return format(LocalDateTime.now());
    }

    public static String format(LocalDateTime time) {
        return time.format(FORMAT);
    }

    public static LocalDateTime parse(String value, String errorMessage) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw AppException.badRequest(errorMessage);
        }
    }
}
