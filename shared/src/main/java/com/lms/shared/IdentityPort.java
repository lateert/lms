package com.lms.shared;

public interface IdentityPort {
    UserSnapshot validateSession(String token);

    UserSnapshot getUser(String userId);
}
