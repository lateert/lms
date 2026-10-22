package com.lms.gateway;

import com.lms.shared.AppException;
import com.lms.shared.UserSnapshot;

final class Actors {
    private Actors() {
    }

    static UserSnapshot require(UserSnapshot user) {
        if (user == null) {
            throw AppException.unauthorized("Сессия недействительна");
        }
        return user;
    }
}
