package com.lms.shared.view;

import com.lms.shared.UserSnapshot;

import java.util.List;

public record UserView(String id, String email, String fullName, String status, List<String> roles) {
    public static UserView from(UserSnapshot user) {
        return new UserView(user.id(), user.email(), user.fullName(), user.status(), user.roles());
    }
}
