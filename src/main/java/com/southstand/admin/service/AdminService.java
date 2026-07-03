package com.southstand.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southstand.admin.dto.AdminContentStatusRequest;
import com.southstand.admin.dto.AdminUserStatusRequest;
import com.southstand.admin.entity.AdminOperationLog;
import com.southstand.admin.mapper.AdminOperationLogMapper;
import com.southstand.admin.vo.AdminContentVO;
import com.southstand.admin.vo.AdminDashboardSummaryVO;
import com.southstand.admin.vo.AdminStatusUpdateVO;
import com.southstand.admin.vo.AdminUserVO;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.entity.FavoriteRecord;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_HIDDEN = "HIDDEN";

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final ContentMapper contentMapper;
    private final CommentMapper commentMapper;
    private final FavoriteRecordMapper favoriteRecordMapper;
    private final MatchInfoMapper matchInfoMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;

    public AdminService(SysUserMapper sysUserMapper,
            UserProfileMapper userProfileMapper,
            ContentMapper contentMapper,
            CommentMapper commentMapper,
            FavoriteRecordMapper favoriteRecordMapper,
            MatchInfoMapper matchInfoMapper,
            AdminOperationLogMapper adminOperationLogMapper) {
        this.sysUserMapper = sysUserMapper;
        this.userProfileMapper = userProfileMapper;
        this.contentMapper = contentMapper;
        this.commentMapper = commentMapper;
        this.favoriteRecordMapper = favoriteRecordMapper;
        this.matchInfoMapper = matchInfoMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    public AdminDashboardSummaryVO dashboardSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        AdminDashboardSummaryVO vo = new AdminDashboardSummaryVO();
        vo.setUserCount(countUsers(null, null, null));
        vo.setActiveUserCount(countUsers(STATUS_ACTIVE, null, null));
        vo.setDisabledUserCount(countUsers(STATUS_DISABLED, null, null));
        vo.setContentCount(countContents(null, null));
        vo.setPublishedContentCount(countContents(STATUS_PUBLISHED, null));
        vo.setHiddenContentCount(countContents(STATUS_HIDDEN, null));
        vo.setCommentCount(commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, STATUS_ACTIVE)
                .eq(Comment::getIsDeleted, 0)));
        vo.setMatchCount(matchInfoMapper.selectCount(new LambdaQueryWrapper<MatchInfo>()
                .eq(MatchInfo::getStatus, STATUS_ACTIVE)
                .eq(MatchInfo::getIsDeleted, 0)));
        vo.setTodayNewUserCount(countUsers(null, null, todayStart));
        vo.setTodayNewContentCount(countContents(null, todayStart));
        return vo;
    }

    public PageResult<AdminUserVO> users(Long pageNum, Long pageSize, String keyword, String status, String roleType) {
        List<Long> nicknameMatchedUserIds = profileUserIdsByNickname(keyword);
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(safePageNum(pageNum), safePageSize(pageSize)),
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getIsDeleted, 0)
                        .eq(!isBlank(status), SysUser::getStatus, status)
                        .eq(!isBlank(roleType), SysUser::getRoleType, roleType)
                        .and(!isBlank(keyword), wrapper -> wrapper
                                .like(SysUser::getUsername, keyword)
                                .or()
                                .like(SysUser::getPhone, keyword)
                                .or(!nicknameMatchedUserIds.isEmpty())
                                .in(!nicknameMatchedUserIds.isEmpty(), SysUser::getId, nicknameMatchedUserIds))
                        .orderByDesc(SysUser::getCreateTime)
                        .orderByDesc(SysUser::getId));
        Map<Long, UserProfile> profileByUserId = profilesByUserId(page.getRecords().stream().map(SysUser::getId).toList());
        List<AdminUserVO> records = page.getRecords().stream()
                .map(user -> toAdminUser(user, profileByUserId.get(user.getId())))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminStatusUpdateVO updateUserStatus(Long userId, AdminUserStatusRequest request) {
        if (userId == null || request == null || !isUserStatus(request.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Long adminUserId = CurrentUserHolder.get().getUserId();
        if (adminUserId.equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "cannot disable current admin user");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        sysUserMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getStatus, request.getStatus()));
        logOperation(adminUserId, "UPDATE_USER_STATUS", "USER", userId,
                "status=" + request.getStatus() + ", reason=" + defaultString(request.getReason(), ""));
        return new AdminStatusUpdateVO(userId, request.getStatus());
    }

    public PageResult<AdminContentVO> contents(Long pageNum, Long pageSize, String keyword,
            String contentType, String status, Long authorId) {
        Page<Content> page = contentMapper.selectPage(new Page<>(safePageNum(pageNum), safePageSize(pageSize)),
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getIsDeleted, 0)
                        .eq(!isBlank(contentType), Content::getContentType, contentType)
                        .eq(!isBlank(status), Content::getStatus, status)
                        .eq(authorId != null, Content::getAuthorId, authorId)
                        .and(!isBlank(keyword), wrapper -> wrapper
                                .like(Content::getTitle, keyword)
                                .or()
                                .like(Content::getSummary, keyword))
                        .orderByDesc(Content::getPublishTime)
                        .orderByDesc(Content::getCreateTime));
        Map<Long, UserProfile> profileByUserId = profilesByUserId(page.getRecords().stream().map(Content::getAuthorId).toList());
        List<AdminContentVO> records = page.getRecords().stream()
                .map(content -> toAdminContent(content, profileByUserId.get(content.getAuthorId())))
                .toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminStatusUpdateVO updateContentStatus(Long contentId, AdminContentStatusRequest request) {
        if (contentId == null || request == null || !isContentStatus(request.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        Content content = contentMapper.selectById(contentId);
        if (content == null || Integer.valueOf(1).equals(content.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "content not found");
        }
        contentMapper.update(null, new LambdaUpdateWrapper<Content>()
                .eq(Content::getId, contentId)
                .set(Content::getStatus, request.getStatus()));
        Long adminUserId = CurrentUserHolder.get().getUserId();
        logOperation(adminUserId, "UPDATE_CONTENT_STATUS", "CONTENT", contentId,
                "status=" + request.getStatus() + ", reason=" + defaultString(request.getReason(), ""));
        return new AdminStatusUpdateVO(contentId, request.getStatus());
    }

    private Long countUsers(String status, String roleType, LocalDateTime createdAfter) {
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getIsDeleted, 0)
                .eq(!isBlank(status), SysUser::getStatus, status)
                .eq(!isBlank(roleType), SysUser::getRoleType, roleType)
                .ge(createdAfter != null, SysUser::getCreateTime, createdAfter));
    }

    private Long countContents(String status, LocalDateTime createdAfter) {
        return contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getIsDeleted, 0)
                .eq(!isBlank(status), Content::getStatus, status)
                .ge(createdAfter != null, Content::getCreateTime, createdAfter));
    }

    private Map<Long, UserProfile> profilesByUserId(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .in(UserProfile::getUserId, userIds)
                        .eq(UserProfile::getIsDeleted, 0))
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity(), (first, ignored) -> first));
    }

    private List<Long> profileUserIdsByNickname(String keyword) {
        if (isBlank(keyword)) {
            return List.of();
        }
        return userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .like(UserProfile::getNickname, keyword)
                        .eq(UserProfile::getIsDeleted, 0))
                .stream()
                .map(UserProfile::getUserId)
                .toList();
    }

    private AdminUserVO toAdminUser(SysUser user, UserProfile profile) {
        AdminUserVO vo = new AdminUserVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(profile == null ? user.getUsername() : defaultString(profile.getNickname(), user.getUsername()));
        vo.setPhoneMasked(maskPhone(user.getPhone()));
        vo.setRoleType(user.getRoleType());
        vo.setStatus(user.getStatus());
        vo.setOnboardingCompleted(Integer.valueOf(1).equals(user.getOnboardingCompleted()));
        vo.setPostCount(countUserContents(user.getId()));
        vo.setFavoriteCount(countUserFavorites(user.getId()));
        vo.setCommentCount(countUserComments(user.getId()));
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private AdminContentVO toAdminContent(Content content, UserProfile profile) {
        AdminContentVO vo = new AdminContentVO();
        vo.setContentId(content.getId());
        vo.setContentType(content.getContentType());
        vo.setTitle(content.getTitle());
        vo.setSummary(defaultString(content.getSummary(), ""));
        vo.setAuthorId(content.getAuthorId());
        vo.setAuthorNickname(profile == null ? "user-" + content.getAuthorId() : profile.getNickname());
        vo.setStatus(content.getStatus());
        vo.setLikeCount(nvl(content.getLikeCount()));
        vo.setCommentCount(nvl(content.getCommentCount()));
        vo.setFavoriteCount(nvl(content.getFavoriteCount()));
        vo.setPublishTime(content.getPublishTime());
        vo.setCreateTime(content.getCreateTime());
        return vo;
    }

    private Integer countUserContents(Long userId) {
        return contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getAuthorId, userId)
                .eq(Content::getIsDeleted, 0)).intValue();
    }

    private Integer countUserFavorites(Long userId) {
        return favoriteRecordMapper.selectCount(new LambdaQueryWrapper<FavoriteRecord>()
                .eq(FavoriteRecord::getUserId, userId)
                .eq(FavoriteRecord::getTargetType, "CONTENT")
                .eq(FavoriteRecord::getStatus, STATUS_ACTIVE)).intValue();
    }

    private Integer countUserComments(Long userId) {
        return commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .eq(Comment::getStatus, STATUS_ACTIVE)
                .eq(Comment::getIsDeleted, 0)).intValue();
    }

    private void logOperation(Long adminUserId, String operationType, String targetType, Long targetId, String desc) {
        try {
            AdminOperationLog log = new AdminOperationLog();
            log.setAdminUserId(adminUserId);
            log.setOperationType(operationType);
            log.setTargetType(targetType);
            log.setTargetId(targetId);
            log.setOperationDesc(desc);
            log.setStatus(STATUS_ACTIVE);
            log.setIsDeleted(0);
            adminOperationLogMapper.insert(log);
        } catch (RuntimeException ignored) {
            // Operation logs are best-effort and must not break status updates.
        }
    }

    private String maskPhone(String phone) {
        if (isBlank(phone)) {
            return "";
        }
        String text = phone.trim();
        if (text.length() < 7) {
            return "****";
        }
        return text.substring(0, 3) + "****" + text.substring(text.length() - 4);
    }

    private boolean isUserStatus(String status) {
        return STATUS_ACTIVE.equals(status) || STATUS_DISABLED.equals(status);
    }

    private boolean isContentStatus(String status) {
        return STATUS_PUBLISHED.equals(status) || STATUS_HIDDEN.equals(status);
    }

    private long safePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private long safePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
