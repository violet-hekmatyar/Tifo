package com.southstand.admin;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.common.result.Result;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminHealthController {

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("roleType", CurrentUserHolder.get().getRoleType());
        data.put("timestamp", OffsetDateTime.now().toString());
        return Result.success(data);
    }
}
