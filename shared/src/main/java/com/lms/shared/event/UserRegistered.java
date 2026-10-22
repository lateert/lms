package com.lms.shared.event;

public record UserRegistered(String userId, String email, String fullName) {
}
