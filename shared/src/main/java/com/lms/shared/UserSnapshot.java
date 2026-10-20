package com.lms.shared;

import java.util.List;

public record UserSnapshot(String id, String email, String fullName, String status, List<String> roles) {
}
