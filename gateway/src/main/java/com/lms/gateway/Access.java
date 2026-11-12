package com.lms.gateway;

import com.lms.shared.AppException;
import com.lms.shared.UserSnapshot;

final class Access {
    private Access() {
    }

    static UserSnapshot allow(UserSnapshot user, String... roles) {
        UserSnapshot actor = Actors.require(user);
        for (String role : roles) {
            if (actor.hasRole(role)) {
                return actor;
            }
        }
        throw AppException.forbidden("Недостаточно прав для этой операции");
    }
}
