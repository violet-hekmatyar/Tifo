package com.southstand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * T01 temporarily excludes datasource auto-configuration.
 * T02 will remove this exclusion when MySQL/Redis health checks are connected.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
public class SouthStandApplication {

    public static void main(String[] args) {
        SpringApplication.run(SouthStandApplication.class, args);
    }
}
