package com.southstand.api;

import com.southstand.common.result.Result;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class HealthController {

    private final Environment environment;
    private final String version;

    public HealthController(Environment environment, @Value("${app.version:0.1.0-SNAPSHOT}") String version) {
        this.environment = environment;
        this.version = version;
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
}
