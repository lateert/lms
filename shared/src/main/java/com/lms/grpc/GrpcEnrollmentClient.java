package com.lms.grpc;

import com.lms.shared.EnrollmentPort;
import com.lms.shared.EnrollmentSnapshot;
import io.grpc.ManagedChannel;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class GrpcEnrollmentClient implements EnrollmentPort {
    private final GrpcProperties properties;
    private final ManagedChannel channel;
    private final EnrollmentApiGrpc.EnrollmentApiBlockingStub stub;

    public GrpcEnrollmentClient(GrpcProperties properties) {
        this.properties = properties;
        this.channel = GrpcMapping.channel(properties.getEnrollment());
        this.stub = EnrollmentApiGrpc.newBlockingStub(channel);
    }

    @Override
    public boolean isEnrolled(String studentId, String courseId) {
        return call("IsEnrolled", () -> stub.withDeadlineAfter(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .isEnrolled(request(studentId, courseId))).getEnrolled();
    }

    @Override
    public EnrollmentSnapshot find(String studentId, String courseId) {
        return GrpcMapping.enrollment(call("FindEnrollment", () -> stub.withDeadlineAfter(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .findEnrollment(request(studentId, courseId))));
    }

    private <T> T call(String operation, java.util.function.Supplier<T> action) {
        return GrpcCalls.call("enrollment", operation, properties, action);
    }

    private static EnrollmentRequest request(String studentId, String courseId) {
        return EnrollmentRequest.newBuilder().setStudentId(studentId).setCourseId(courseId).build();
    }

    @PreDestroy
    public void close() {
        GrpcMapping.shutdown(channel);
    }
}
