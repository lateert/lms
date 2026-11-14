package com.lms.grpc;

import com.lms.shared.CatalogPort;
import com.lms.shared.CourseCounts;
import com.lms.shared.CourseSnapshot;
import io.grpc.ManagedChannel;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class GrpcCatalogClient implements CatalogPort {
    private final ManagedChannel channel;
    private final CatalogApiGrpc.CatalogApiBlockingStub stub;

    public GrpcCatalogClient(GrpcProperties properties) {
        this.channel = GrpcMapping.channel(properties.getCatalog());
        this.stub = CatalogApiGrpc.newBlockingStub(channel);
    }

    @Override
    public CourseSnapshot getPublishedCourse(String courseId) {
        CourseReply reply = GrpcCalls.call("catalog", () -> stub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .getPublishedCourse(CourseIdRequest.newBuilder().setCourseId(courseId).build()));
        return "published".equals(reply.getStatus()) ? GrpcMapping.course(reply) : null;
    }

    @Override
    public CourseSnapshot getCourse(String courseId) {
        return GrpcMapping.course(GrpcCalls.call("catalog", () -> stub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .getCourse(CourseIdRequest.newBuilder().setCourseId(courseId).build())));
    }

    @Override
    public boolean lessonExists(String lessonId) {
        return GrpcCalls.call("catalog", () -> stub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .lessonExists(LessonIdRequest.newBuilder().setLessonId(lessonId).build())).getExists();
    }

    @Override
    public String lessonCourseId(String lessonId) {
        LessonReply reply = GrpcCalls.call("catalog", () -> stub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .lessonExists(LessonIdRequest.newBuilder().setLessonId(lessonId).build()));
        return reply.getExists() ? reply.getCourseId() : null;
    }

    @Override
    public List<String> listLessonIds(String courseId) {
        return GrpcCalls.call("catalog", () -> stub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listLessonIds(CourseIdRequest.newBuilder().setCourseId(courseId).build())).getLessonIdsList();
    }

    @Override
    public CourseCounts getCourseCounts(String courseId) {
        return GrpcMapping.counts(GrpcCalls.call("catalog", () -> stub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .getCourseCounts(CourseIdRequest.newBuilder().setCourseId(courseId).build())));
    }

    @PreDestroy
    public void close() {
        GrpcMapping.shutdown(channel);
    }
}
