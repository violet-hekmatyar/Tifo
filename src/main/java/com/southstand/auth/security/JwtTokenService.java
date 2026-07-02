package com.southstand.auth.security;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(LoginUserContext userContext) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getAccessTokenExpireSeconds());
        return Jwts.builder()
                .subject(userContext.getUsername())
                .claim("userId", userContext.getUserId())
                .claim("username", userContext.getUsername())
                .claim("roleType", userContext.getRoleType())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public LoginUserContext parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            String roleType = claims.get("roleType", String.class);
            if (userId == null || username == null || roleType == null) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            return new LoginUserContext(userId, username, roleType);
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    public long getAccessTokenExpireSeconds() {
        return properties.getAccessTokenExpireSeconds();
    }
}
