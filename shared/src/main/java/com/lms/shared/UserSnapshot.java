package com.lms.shared;

import java.util.List;

public record UserSnapshot(String id, String email, String fullName, String status, List<String> roles) {
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(Roles.ADMIN);
    }
}
