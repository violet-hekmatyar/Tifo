package com.southstand.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.southstand.auth.dto.RegisterRequest;
import com.southstand.auth.security.JwtTokenService;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.auth.service.AuthService;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.infrastructure.cache.LoginFailCacheService;
import com.southstand.user.entity.SysUser;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserOnboardingMapper;
import com.southstand.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTests {

    @Test
    void bcryptDoesNotStorePlainPassword() {
        String rawPassword = "123456";
        String hash = new BCryptPasswordEncoder().encode(rawPassword);

        assertThat(hash).isNotEqualTo(rawPassword);
        assertThat(hash).startsWith("$2");
        assertThat(new BCryptPasswordEncoder().matches(rawPassword, hash)).isTrue();
    }

    @Test
    void duplicateUsernameReturnsConflict() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AuthService authService = new AuthService(
                userMapper,
                mock(UserProfileMapper.class),
                mock(UserOnboardingMapper.class),
                mock(JwtTokenService.class),
                mock(LoginFailCacheService.class)
        );
        when(userMapper.selectCount(any())).thenReturn(1L);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("test_user");
        request.setPassword("123456");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void adminRoleCheckOnlyAcceptsAdmin() {
        AuthService authService = new AuthService(
                mock(SysUserMapper.class),
                mock(UserProfileMapper.class),
                mock(UserOnboardingMapper.class),
                mock(JwtTokenService.class),
                mock(LoginFailCacheService.class)
        );

        assertThat(authService.hasAdminRole(new LoginUserContext(1L, "admin", "ADMIN"))).isTrue();
        assertThat(authService.hasAdminRole(new LoginUserContext(2L, "user", "USER"))).isFalse();
    }

    @Test
    void disabledUserCannotLogin() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        LoginFailCacheService failCacheService = mock(LoginFailCacheService.class);
        AuthService authService = new AuthService(
                userMapper,
                mock(UserProfileMapper.class),
                mock(UserOnboardingMapper.class),
                mock(JwtTokenService.class),
                failCacheService
        );

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("disabled_user");
        user.setPasswordHash(new BCryptPasswordEncoder().encode("123456"));
        user.setStatus("DISABLED");
        user.setIsDeleted(0);
        when(failCacheService.isLocked("disabled_user")).thenReturn(false);
        when(userMapper.selectOne(any())).thenReturn(user);

        com.southstand.auth.dto.LoginRequest request = new com.southstand.auth.dto.LoginRequest();
        request.setUsername("disabled_user");
        request.setPassword("123456");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
