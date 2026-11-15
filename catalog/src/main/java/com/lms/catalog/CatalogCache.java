package com.lms.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.shared.CourseCounts;
import com.lms.shared.CourseSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogCache {
    public static final String COURSE = "lms:course:";
    public static final String COUNTS = "lms:course-counts:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public CourseSnapshot course(String courseId) {
        return read(COURSE + courseId, CourseSnapshot.class);
    }

    public void putCourse(CourseSnapshot course) {
        write(COURSE + course.id(), course);
    }

    public CourseCounts counts(String courseId) {
        return read(COUNTS + courseId, CourseCounts.class);
    }

    public void putCounts(String courseId, CourseCounts counts) {
        write(COUNTS + courseId, counts);
    }

    public void evict(String courseId) {
        try {
            redis.delete(COURSE + courseId);
            redis.delete(COUNTS + courseId);
            log.info("Кеш курса {} сброшен", courseId);
        } catch (RuntimeException ex) {
            log.error("Не удалось сбросить кеш курса {}", courseId, ex);
        }
    }

    private <T> T read(String key, Class<T> type) {
        try {
            String raw = redis.opsForValue().get(key);
            if (raw == null) {
                return null;
            }
            log.info("Кеш {} найден", key);
            return json.readValue(raw, type);
        } catch (Exception ex) {
            log.error("Кеш {} не прочитан, данные берутся из базы", key, ex);
            return null;
        }
    }

    private void write(String key, Object value) {
        try {
            redis.opsForValue().set(key, json.writeValueAsString(value), TTL);
            log.info("Кеш {} записан на {} мин", key, TTL.toMinutes());
        } catch (Exception ex) {
            log.error("Кеш {} не записан, ответ всё равно отдан из базы", key, ex);
        }
    }
}
