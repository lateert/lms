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
    private final GrpcProperties properties;
    private final ManagedChannel channel;
    private final CatalogApiGrpc.CatalogApiBlockingStub stub;

    public GrpcCatalogClient(GrpcProperties properties) {
        this.properties = properties;
        this.channel = GrpcMapping.channel(properties.getCatalog());
        this.stub = CatalogApiGrpc.newBlockingStub(channel);
    }

    @Override
    public CourseSnapshot getPublishedCourse(String courseId) {
        CourseReply reply = call("GetPublishedCourse", () -> stub.withDeadlineAfter(timeout(), TimeUnit.MILLISECONDS)
                .getPublishedCourse(CourseIdRequest.newBuilder().setCourseId(courseId).build()));
        return "published".equals(reply.getStatus()) ? GrpcMapping.course(reply) : null;
    }

    @Override
    public CourseSnapshot getCourse(String courseId) {
        return GrpcMapping.course(call("GetCourse", () -> stub.withDeadlineAfter(timeout(), TimeUnit.MILLISECONDS)
                .getCourse(CourseIdRequest.newBuilder().setCourseId(courseId).build())));
    }

    @Override
    public boolean lessonExists(String lessonId) {
        return call("LessonExists", () -> stub.withDeadlineAfter(timeout(), TimeUnit.MILLISECONDS)
                .lessonExists(LessonIdRequest.newBuilder().setLessonId(lessonId).build())).getExists();
    }

    @Override
    public String lessonCourseId(String lessonId) {
        LessonReply reply = call("LessonExists", () -> stub.withDeadlineAfter(timeout(), TimeUnit.MILLISECONDS)
                .lessonExists(LessonIdRequest.newBuilder().setLessonId(lessonId).build()));
        return reply.getExists() ? reply.getCourseId() : null;
    }

    @Override
    public List<String> listLessonIds(String courseId) {
        return call("ListLessonIds", () -> stub.withDeadlineAfter(timeout(), TimeUnit.MILLISECONDS)
                .listLessonIds(CourseIdRequest.newBuilder().setCourseId(courseId).build())).getLessonIdsList();
    }

    @Override
    public CourseCounts getCourseCounts(String courseId) {
        return GrpcMapping.counts(call("GetCourseCounts", () -> stub.withDeadlineAfter(timeout(), TimeUnit.MILLISECONDS)
                .getCourseCounts(CourseIdRequest.newBuilder().setCourseId(courseId).build())));
    }

    private long timeout() {
        return properties.getTimeout().toMillis();
    }

    private <T> T call(String operation, java.util.function.Supplier<T> action) {
        return GrpcCalls.call("catalog", operation, properties, action);
    }

    @PreDestroy
    public void close() {
        GrpcMapping.shutdown(channel);
    }
}
