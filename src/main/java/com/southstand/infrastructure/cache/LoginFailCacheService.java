package com.southstand.infrastructure.cache;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class LoginFailCacheService {

    private static final int FAIL_THRESHOLD = 5;
    private static final Duration FAIL_WINDOW = Duration.ofMinutes(5);
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);
    private static final String FAIL_PREFIX = "login:fail:";
    private static final String LOCK_PREFIX = "login:lock:";

    private final StringRedisTemplate redisTemplate;

    public LoginFailCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLocked(String usernameOrIp) {
        Boolean exists = redisTemplate.hasKey(lockKey(usernameOrIp));
        return Boolean.TRUE.equals(exists);
    }

    public void recordFailure(String usernameOrIp) {
        String key = failKey(usernameOrIp);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, FAIL_WINDOW);
        }
        if (count != null && count >= FAIL_THRESHOLD) {
            redisTemplate.opsForValue().set(lockKey(usernameOrIp), "1", LOCK_TTL);
        }
    }

    public void clear(String usernameOrIp) {
        redisTemplate.delete(failKey(usernameOrIp));
        redisTemplate.delete(lockKey(usernameOrIp));
    }

    private String failKey(String usernameOrIp) {
        return FAIL_PREFIX + usernameOrIp;
    }

    private String lockKey(String usernameOrIp) {
        return LOCK_PREFIX + usernameOrIp;
    }
}
