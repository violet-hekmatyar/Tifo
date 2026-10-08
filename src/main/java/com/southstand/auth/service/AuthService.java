package com.southstand.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.southstand.auth.dto.LoginRequest;
import com.southstand.auth.dto.RegisterRequest;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.JwtTokenService;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.auth.vo.LoginResponse;
import com.southstand.auth.vo.UserInfoVO;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.infrastructure.cache.LoginFailCacheService;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserOnboarding;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserOnboardingMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final String ROLE_USER = "USER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserOnboardingMapper userOnboardingMapper;
    private final JwtTokenService jwtTokenService;
    private final LoginFailCacheService loginFailCacheService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            SysUserMapper sysUserMapper,
            UserProfileMapper userProfileMapper,
            UserOnboardingMapper userOnboardingMapper,
            JwtTokenService jwtTokenService,
            LoginFailCacheService loginFailCacheService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.userProfileMapper = userProfileMapper;
        this.userOnboardingMapper = userOnboardingMapper;
        this.jwtTokenService = jwtTokenService;
        this.loginFailCacheService = loginFailCacheService;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String phone = StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null;

        if (existsByUsername(username)) {
            throw new BusinessException(ErrorCode.CONFLICT, "username already exists");
        }
        if (StringUtils.hasText(phone) && existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.CONFLICT, "phone already exists");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoleType(ROLE_USER);
        user.setOnboardingCompleted(0);
        user.setStatus(STATUS_ACTIVE);
        user.setIsDeleted(0);
        sysUserMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setNickname(username);
        profile.setStatus(STATUS_ACTIVE);
        profile.setIsDeleted(0);
        profile.setPostCount(0);
        profile.setFollowerCount(0);
        profile.setFollowingCount(0);
        profile.setTeamFollowCount(0);
        profile.setPlayerFollowCount(0);
        userProfileMapper.insert(profile);

        UserOnboarding onboarding = new UserOnboarding();
        onboarding.setUserId(user.getId());
        onboarding.setSelectedTeamIds("[]");
        onboarding.setSelectedPlayerIds("[]");
        onboarding.setCompleted(0);
        onboarding.setStatus(STATUS_ACTIVE);
        onboarding.setIsDeleted(0);
        userOnboardingMapper.insert(onboarding);

        return toUserInfo(user, profile);
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        if (loginFailCacheService.isLocked(username)) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED_TOO_MANY_TIMES, "登录失败次数过多，请稍后再试");
        }

        SysUser user = findByUsername(username);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginFailCacheService.recordFailure(username);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "username or password is incorrect");
        }
        if (STATUS_DISABLED.equals(user.getStatus()) || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "user is disabled");
        }

        loginFailCacheService.clear(username);
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getLastLoginTime, LocalDateTime.now()));

        LoginUserContext context = new LoginUserContext(user.getId(), user.getUsername(), user.getRoleType());
        String accessToken = jwtTokenService.generate(context);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(jwtTokenService.getAccessTokenExpireSeconds());
        response.setUser(getUserInfo(user.getId()));
        return response;
    }

    public UserInfoVO me() {
        LoginUserContext current = CurrentUserHolder.get();
        return getUserInfo(current.getUserId());
    }

    public UserInfoVO getUserInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        return toUserInfo(user, profile);
    }

    public boolean hasAdminRole(LoginUserContext userContext) {
        return "ADMIN".equals(userContext.getRoleType());
    }

    private boolean existsByUsername(String username) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getIsDeleted, 0));
        return count != null && count > 0;
    }

    private boolean existsByPhone(String phone) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, phone)
                .eq(SysUser::getIsDeleted, 0));
        return count != null && count > 0;
    }

    private SysUser findByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    private UserInfoVO toUserInfo(SysUser user, UserProfile profile) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhoneMasked(maskPhone(user.getPhone()));
        vo.setRoleType(user.getRoleType());
        vo.setStatus(user.getStatus());
        vo.setOnboardingCompleted(Integer.valueOf(1).equals(user.getOnboardingCompleted()));
        if (profile != null) {
            vo.setNickname(profile.getNickname());
            vo.setAvatarUrl(profile.getAvatarUrl());
            vo.setMainTeamId(profile.getMainTeamId());
        }
        return vo;
    }

    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String trimmed = phone.trim();
        String digits = trimmed.replaceAll("[^0-9]", "");
        boolean mainlandNumber = digits.length() == 11
                && !trimmed.startsWith("+")
                && !trimmed.startsWith("00");
        if (digits.length() == 13 && trimmed.startsWith("+86")) {
            digits = digits.substring(2);
            mainlandNumber = true;
        } else if (digits.length() == 15 && trimmed.startsWith("0086")) {
            digits = digits.substring(4);
            mainlandNumber = true;
        }
        if (mainlandNumber) {
            return "+86 " + digits.substring(0, 3) + "****" + digits.substring(7);
        }
        if (digits.length() >= 7 && digits.length() <= 15) {
            return digits.substring(0, 2) + "****" + digits.substring(digits.length() - 2);
        }
        return "****";
    }
}
