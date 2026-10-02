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
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Test
    void meReturnsOnlySafelyMaskedPhone() throws Exception {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        AuthService authService = new AuthService(
                userMapper,
                profileMapper,
                mock(UserOnboardingMapper.class),
                mock(JwtTokenService.class),
                mock(LoginFailCacheService.class)
        );

        SysUser user = new SysUser();
        user.setId(77L);
        user.setUsername("account_user");
        user.setPhone("18512349583");
        user.setPasswordHash("never-return-this");
        user.setStatus("ACTIVE");
        user.setIsDeleted(0);
        when(userMapper.selectById(77L)).thenReturn(user);
        when(profileMapper.selectOne(any())).thenReturn(null);

        var result = authService.getUserInfo(77L);
        assertThat(result.getPhoneMasked()).isEqualTo("+86 185****9583");
        String json = new ObjectMapper().writeValueAsString(result);
        assertThat(json).contains("+86 185****9583");
        assertThat(json).doesNotContain("18512349583");
        assertThat(json).doesNotContain("passwordHash");
        assertThat(json).doesNotContain("never-return-this");
    }

    @Test
    void phoneMaskHandlesMissingAndNonMainlandValuesWithoutEchoingInput() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        AuthService authService = new AuthService(
                userMapper,
                mock(UserProfileMapper.class),
                mock(UserOnboardingMapper.class),
                mock(JwtTokenService.class),
                mock(LoginFailCacheService.class)
        );

        SysUser user = new SysUser();
        user.setId(78L);
        user.setUsername("masked_user");
        user.setPhone("+1 202 555 0199");
        user.setStatus("ACTIVE");
        user.setIsDeleted(0);
        when(userMapper.selectById(78L)).thenReturn(user);

        assertThat(authService.getUserInfo(78L).getPhoneMasked()).isEqualTo("12****99");
        user.setPhone("+86 18512349583");
        assertThat(authService.getUserInfo(78L).getPhoneMasked()).isEqualTo("+86 185****9583");
        user.setPhone("1234");
        assertThat(authService.getUserInfo(78L).getPhoneMasked()).isEqualTo("****");
        user.setPhone(null);
        assertThat(authService.getUserInfo(78L).getPhoneMasked()).isNull();
    }
}
