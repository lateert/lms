package com.lms.grpc;

import com.lms.shared.CourseCounts;
import com.lms.shared.CourseSnapshot;
import com.lms.shared.EnrollmentSnapshot;
import com.lms.shared.UserSnapshot;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

import java.util.concurrent.TimeUnit;

public final class GrpcMapping {
    private GrpcMapping() {
    }

    public static io.grpc.ManagedChannel channel(String target) {
        return NettyChannelBuilder.forTarget(target).usePlaintext().build();
    }

    public static void shutdown(io.grpc.ManagedChannel channel) {
        channel.shutdown();
        try {
            channel.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static UserSnapshot user(UserReply reply) {
        if (reply == null || !reply.getFound()) {
            return null;
        }
        return new UserSnapshot(reply.getUserId(), reply.getEmail(), reply.getFullName(), reply.getStatus(), reply.getRolesList());
    }

    public static UserReply userReply(UserSnapshot user) {
        if (user == null) {
            return UserReply.getDefaultInstance();
        }
        return UserReply.newBuilder()
                .setFound(true)
                .setUserId(user.id())
                .setEmail(nullToEmpty(user.email()))
                .setFullName(nullToEmpty(user.fullName()))
                .setStatus(nullToEmpty(user.status()))
                .addAllRoles(user.roles())
                .build();
    }

    public static CourseSnapshot course(CourseReply reply) {
        if (reply == null || !reply.getFound()) {
            return null;
        }
        return new CourseSnapshot(
                reply.getId(), reply.getTitle(), reply.getDifficulty(), reply.getStatus(), reply.getRating(),
                reply.getCategoryId(), reply.getCategoryName(), reply.getAuthorId()
        );
    }

    public static CourseReply courseReply(CourseSnapshot course) {
        if (course == null) {
            return CourseReply.getDefaultInstance();
        }
        return CourseReply.newBuilder()
                .setFound(true)
                .setId(course.id())
                .setTitle(nullToEmpty(course.title()))
                .setDifficulty(nullToEmpty(course.difficulty()))
                .setStatus(nullToEmpty(course.status()))
                .setRating(course.rating())
                .setCategoryId(nullToEmpty(course.categoryId()))
                .setCategoryName(nullToEmpty(course.categoryName()))
                .setAuthorId(nullToEmpty(course.authorId()))
                .build();
    }

    public static EnrollmentSnapshot enrollment(EnrollmentReply reply) {
        if (reply == null || !reply.getFound()) {
            return null;
        }
        return new EnrollmentSnapshot(
                reply.getEnrollmentId(), reply.getStudentId(), reply.getCourseId(), reply.getStatus(), reply.getEnrolledAt()
        );
    }

    public static CourseCounts counts(CourseCountsReply reply) {
        return new CourseCounts(reply.getModuleCount(), reply.getLessonCount());
    }

    public static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
