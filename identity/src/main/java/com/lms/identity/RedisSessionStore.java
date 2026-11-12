package com.lms.identity;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisSessionStore {
    private static final String PREFIX = "lms:session:";

    private final StringRedisTemplate redis;
    private final AuthProperties auth;

    public String open(String userId) {
        String sessionId = UUID.randomUUID().toString();
        redis.opsForValue().set(PREFIX + sessionId, userId, auth.getSessionTtl());
        return sessionId;
    }

    public boolean active(String sessionId, String userId) {
        if (sessionId == null || userId == null) {
            return false;
        }
        return userId.equals(redis.opsForValue().get(PREFIX + sessionId));
    }

    public void close(String sessionId) {
        if (sessionId != null) {
            redis.delete(PREFIX + sessionId);
        }
    }
}
