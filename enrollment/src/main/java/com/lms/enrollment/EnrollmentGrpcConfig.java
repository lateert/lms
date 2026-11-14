package com.lms.enrollment;

import com.lms.grpc.EnrolledReply;
import com.lms.grpc.EnrollmentApiGrpc;
import com.lms.grpc.EnrollmentReply;
import com.lms.grpc.EnrollmentRequest;
import com.lms.grpc.GrpcCalls;
import com.lms.grpc.GrpcMapping;
import com.lms.grpc.GrpcProperties;
import com.lms.grpc.GrpcServer;
import com.lms.shared.EnrollmentSnapshot;
import io.grpc.stub.StreamObserver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
public class EnrollmentGrpcConfig {
    @Bean
    GrpcServer enrollmentGrpcServer(GrpcProperties properties, EnrollmentService enrollment) {
        return new GrpcServer(properties.getEnrollment(), new EnrollmentApiGrpc.EnrollmentApiImplBase() {
            @Override
            public void isEnrolled(EnrollmentRequest request, StreamObserver<EnrolledReply> response) {
                reply(response, () -> {
                    EnrollmentSnapshot found = enrollment.find(request.getStudentId(), request.getCourseId());
                    boolean enrolled = found != null && !"dropped".equals(found.status());
                    EnrolledReply.Builder builder = EnrolledReply.newBuilder().setEnrolled(enrolled);
                    if (enrolled) {
                        builder.setEnrollmentId(found.id());
                    }
                    return builder.build();
                });
            }

            @Override
            public void findEnrollment(EnrollmentRequest request, StreamObserver<EnrollmentReply> response) {
                reply(response, () -> {
                    EnrollmentSnapshot found = enrollment.find(request.getStudentId(), request.getCourseId());
                    if (found == null) {
                        return EnrollmentReply.getDefaultInstance();
                    }
                    return EnrollmentReply.newBuilder()
                            .setFound(true)
                            .setEnrollmentId(found.id())
                            .setStudentId(found.studentId())
                            .setCourseId(found.courseId())
                            .setStatus(found.status())
                            .setEnrolledAt(GrpcMapping.nullToEmpty(found.enrolledAt()))
                            .build();
                });
            }
        });
    }

    private static <T> void reply(StreamObserver<T> response, Supplier<T> body) {
        try {
            response.onNext(GrpcCalls.guard(body));
            response.onCompleted();
        } catch (RuntimeException ex) {
            response.onError(ex);
        }
    }
}
