package com.southstand.common.config;

import com.southstand.auth.security.JwtAccessDeniedHandler;
import com.southstand.auth.security.JwtAuthenticationEntryPoint;
import com.southstand.auth.security.JwtAuthenticationFilter;
import com.southstand.auth.security.JwtProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/app/feed", "/api/app/feed/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/app/contents/**", "/api/app/comments", "/api/app/comments/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/app/search/**").permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/app/users/*/profile",
                                "/api/app/users/*/contents",
                                "/api/app/users/*/followings",
                                "/api/app/users/*/followers"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/app/football/matches/following-teams").authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/app/football/leagues",
                                "/api/app/football/matches",
                                "/api/app/football/matches/**",
                                "/api/app/football/teams/**",
                                "/api/app/football/players/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/public/**",
                                "/doc.html",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/demo/**",
                                "/favicon.ico"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(Customizer.withDefaults());

        return http.build();
    }
}
