package com.lms.shared;

public interface EnrollmentPort {
    boolean isEnrolled(String studentId, String courseId);

    EnrollmentSnapshot find(String studentId, String courseId);
}
