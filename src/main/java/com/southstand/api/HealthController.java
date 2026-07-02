package com.southstand.api;

import com.southstand.common.result.Result;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class HealthController {

    private final Environment environment;
    private final String version;
    private final JdbcTemplate jdbcTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public HealthController(
            Environment environment,
            @Value("${app.version:0.1.0-SNAPSHOT}") String version,
            JdbcTemplate jdbcTemplate,
            RedisConnectionFactory redisConnectionFactory) {
        this.environment = environment;
        this.version = version;
        this.jdbcTemplate = jdbcTemplate;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("appName", "south-stand-server");
        data.put("version", version);
        data.put("profile", String.join(",", environment.getActiveProfiles()));
        data.put("timestamp", OffsetDateTime.now().toString());
        return Result.success(data);
    }

    @GetMapping("/health/db")
    public Result<Map<String, Object>> databaseHealth() {
        Integer value = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        if (value == null || value != 1) {
            throw new IllegalStateException("database health check failed");
        }

        Map<String, Object> data = baseHealthData();
        data.put("status", "UP");
        data.put("database", "UP");
        return Result.success(data);
    }

    @GetMapping("/health/redis")
    public Result<Map<String, Object>> redisHealth() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            if (!"PONG".equalsIgnoreCase(pong)) {
                throw new IllegalStateException("redis health check failed");
            }
        } catch (RedisConnectionFailureException ex) {
            throw new IllegalStateException("redis health check failed", ex);
        }

        Map<String, Object> data = baseHealthData();
        data.put("status", "UP");
        data.put("redis", "UP");
        return Result.success(data);
    }

    private Map<String, Object> baseHealthData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("profile", String.join(",", environment.getActiveProfiles()));
        data.put("timestamp", OffsetDateTime.now().toString());
        return data;
    }
}
