package com.lms.shared;

public interface IdentityPort {
    UserSnapshot validateSession(String userId);

    UserSnapshot getUser(String userId);
}
