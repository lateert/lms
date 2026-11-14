package com.lms.catalog;

import com.lms.grpc.CatalogApiGrpc;
import com.lms.grpc.CourseCountsReply;
import com.lms.grpc.CourseIdRequest;
import com.lms.grpc.CourseReply;
import com.lms.grpc.GrpcCalls;
import com.lms.grpc.GrpcMapping;
import com.lms.grpc.GrpcProperties;
import com.lms.grpc.GrpcServer;
import com.lms.grpc.LessonIdRequest;
import com.lms.grpc.LessonIdsReply;
import com.lms.grpc.LessonReply;
import com.lms.shared.CourseCounts;
import io.grpc.stub.StreamObserver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
public class CatalogGrpcConfig {
    @Bean
    GrpcServer catalogGrpcServer(GrpcProperties properties, CatalogService catalog) {
        return new GrpcServer(properties.getCatalog(), new CatalogApiGrpc.CatalogApiImplBase() {
            @Override
            public void getPublishedCourse(CourseIdRequest request, StreamObserver<CourseReply> response) {
                reply(response, () -> GrpcMapping.courseReply(catalog.getPublishedCourse(request.getCourseId())));
            }

            @Override
            public void getCourse(CourseIdRequest request, StreamObserver<CourseReply> response) {
                reply(response, () -> GrpcMapping.courseReply(catalog.getCourse(request.getCourseId())));
            }

            @Override
            public void lessonExists(LessonIdRequest request, StreamObserver<LessonReply> response) {
                reply(response, () -> {
                    String courseId = catalog.lessonCourseId(request.getLessonId());
                    if (courseId == null) {
                        return LessonReply.getDefaultInstance();
                    }
                    return LessonReply.newBuilder().setExists(true).setCourseId(courseId).build();
                });
            }

            @Override
            public void listLessonIds(CourseIdRequest request, StreamObserver<LessonIdsReply> response) {
                reply(response, () -> LessonIdsReply.newBuilder()
                        .addAllLessonIds(catalog.listLessonIds(request.getCourseId()))
                        .build());
            }

            @Override
            public void getCourseCounts(CourseIdRequest request, StreamObserver<CourseCountsReply> response) {
                reply(response, () -> {
                    CourseCounts counts = catalog.getCourseCounts(request.getCourseId());
                    return CourseCountsReply.newBuilder()
                            .setModuleCount(counts.moduleCount())
                            .setLessonCount(counts.lessonCount())
                            .setAssignmentCount(0)
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
