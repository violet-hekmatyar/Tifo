package com.southstand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;

/**
 * South Stand backend application.
 */
@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class
})
@MapperScan("com.southstand.**.mapper")
public class SouthStandApplication {

    public static void main(String[] args) {
        SpringApplication.run(SouthStandApplication.class, args);
    }
}
