package com.southstand.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.southstand.auth.security.JwtProperties;
import com.southstand.auth.security.JwtTokenService;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTests {

    @Test
    void generateAndParseToken() {
        JwtTokenService service = jwtService(604800L);

        String token = service.generate(new LoginUserContext(10002L, "test_user", "USER"));
        LoginUserContext parsed = service.parse(token);

        assertThat(parsed.getUserId()).isEqualTo(10002L);
        assertThat(parsed.getUsername()).isEqualTo("test_user");
        assertThat(parsed.getRoleType()).isEqualTo("USER");
        assertThat(token).doesNotContain("password");
    }

    @Test
    void parseInvalidTokenReturnsUnauthorized() {
        JwtTokenService service = jwtService(604800L);

        assertThatThrownBy(() -> service.parse("bad-token"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void parseExpiredTokenReturnsTokenExpired() {
        JwtTokenService service = jwtService(-1L);
        String token = service.generate(new LoginUserContext(10002L, "test_user", "USER"));

        assertThatThrownBy(() -> service.parse(token))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    private JwtTokenService jwtService(long expireSeconds) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit_test_jwt_secret_must_be_long_enough_2026");
        properties.setAccessTokenExpireSeconds(expireSeconds);
        return new JwtTokenService(properties);
    }
}
