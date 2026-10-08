package com.southstand.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.follow.entity.FollowRecord;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.follow.service.FollowService;
import com.southstand.notification.service.NotificationService;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import com.southstand.user.vo.MyCommentVO;
import com.southstand.user.vo.MyContentVO;
import com.southstand.user.vo.MyFavoriteVO;
import com.southstand.user.vo.PlayerBriefVO;
import com.southstand.user.vo.TeamBriefVO;
import com.southstand.user.vo.UserFollowActionVO;
import com.southstand.user.vo.UserFollowItemVO;
import com.southstand.user.vo.UserProfileVO;
import com.southstand.user.vo.UserPublicProfileVO;
import com.southstand.user.vo.UserRelationStatus;
import com.southstand.user.vo.UserStandVO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSocialService {

    private static final String ACTIVE = "ACTIVE";
    private static final String PUBLISHED = "PUBLISHED";
    private static final int NOT_DELETED = 0;

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final FollowRecordMapper followRecordMapper;
    private final FollowService followService;
    private final ContentMapper contentMapper;
    private final FootballTeamMapper footballTeamMapper;
    private final FootballPlayerMapper footballPlayerMapper;
    private final UserProfileService userProfileService;
    private final NotificationService notificationService;

    @Autowired
    public UserSocialService(
            SysUserMapper sysUserMapper,
            UserProfileMapper userProfileMapper,
            FollowRecordMapper followRecordMapper,
            FollowService followService,
            ContentMapper contentMapper,
            FootballTeamMapper footballTeamMapper,
            FootballPlayerMapper footballPlayerMapper,
            UserProfileService userProfileService,
            NotificationService notificationService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.userProfileMapper = userProfileMapper;
        this.followRecordMapper = followRecordMapper;
        this.followService = followService;
        this.contentMapper = contentMapper;
        this.footballTeamMapper = footballTeamMapper;
        this.footballPlayerMapper = footballPlayerMapper;
        this.userProfileService = userProfileService;
        this.notificationService = notificationService;
    }

    public UserSocialService(SysUserMapper sysUserMapper, UserProfileMapper userProfileMapper,
            FollowRecordMapper followRecordMapper, FollowService followService, ContentMapper contentMapper,
            FootballTeamMapper footballTeamMapper, FootballPlayerMapper footballPlayerMapper,
            UserProfileService userProfileService) {
        this(sysUserMapper,userProfileMapper,followRecordMapper,followService,contentMapper,footballTeamMapper,
                footballPlayerMapper,userProfileService,null);
    }

    public UserPublicProfileVO publicProfile(Long targetUserId) {
        SysUser target = requireExistingUser(targetUserId);
        Long viewerId = currentUserIdOrNull();
        return toPublicProfile(target, profileOf(target.getId()), viewerId);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserFollowActionVO follow(Long targetUserId) {
        SysUser current = requireActiveCurrentUser();
        requireVisibleUser(targetUserId);
        if (Objects.equals(current.getId(), targetUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "cannot follow yourself");
        }

        FollowRecord record = findRecord(current.getId(), targetUserId);
        if (record == null) {
            FollowRecord created = new FollowRecord();
            created.setUserId(current.getId());
            created.setFollowType(FollowService.TYPE_USER);
            created.setTargetId(targetUserId);
            created.setIsMain(0);
            created.setStatus(FollowService.STATUS_ACTIVE);
            created.setIsDeleted(0);
            followRecordMapper.insert(created);
        } else if (!isActiveFollow(record)) {
            followRecordMapper.update(null, new UpdateWrapper<FollowRecord>()
                    .eq("id", record.getId())
                    .set("status", FollowService.STATUS_ACTIVE)
                    .set("is_deleted", 0)
                    .set("is_main", 0));
        }
        syncUserCounts(current.getId(), targetUserId);
        if (notificationService != null) notificationService.notifyUserFollowed(current.getId(), targetUserId);
        return actionResult(current.getId(), targetUserId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserFollowActionVO unfollow(Long targetUserId) {
        SysUser current = requireActiveCurrentUser();
        requireVisibleUser(targetUserId);
        if (Objects.equals(current.getId(), targetUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "cannot unfollow yourself");
        }

        FollowRecord record = findRecord(current.getId(), targetUserId);
        if (record != null && isActiveFollow(record)) {
            followRecordMapper.update(null, new UpdateWrapper<FollowRecord>()
                    .eq("id", record.getId())
                    .set("status", FollowService.STATUS_CANCELLED)
                    .set("is_main", 0));
        }
        syncUserCounts(current.getId(), targetUserId);
        return actionResult(current.getId(), targetUserId, false);
    }

    public PageResult<UserFollowItemVO> followings(Long userId, Long pageNum, Long pageSize) {
        requireVisibleUser(userId);
        Long viewerId = currentUserIdOrNull();
        Page<FollowRecord> page = followRecordMapper.selectPage(new Page<>(safePageNum(pageNum), safePageSize(pageSize)),
                activeFollowQuery()
                        .eq(FollowRecord::getUserId, userId)
                        .orderByDesc(FollowRecord::getUpdateTime)
                        .orderByDesc(FollowRecord::getId));
        List<UserFollowItemVO> records = toFollowItems(page.getRecords(), true, viewerId);
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<UserFollowItemVO> followers(Long userId, Long pageNum, Long pageSize) {
        requireVisibleUser(userId);
        Long viewerId = currentUserIdOrNull();
        Page<FollowRecord> page = followRecordMapper.selectPage(new Page<>(safePageNum(pageNum), safePageSize(pageSize)),
                activeFollowQuery()
                        .eq(FollowRecord::getTargetId, userId)
                        .orderByDesc(FollowRecord::getUpdateTime)
                        .orderByDesc(FollowRecord::getId));
        List<UserFollowItemVO> records = toFollowItems(page.getRecords(), false, viewerId);
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<MyContentVO> contents(Long userId, Long pageNum, Long pageSize) {
        requireVisibleUser(userId);
        Page<Content> page = contentMapper.selectPage(new Page<>(safePageNum(pageNum), safePageSize(pageSize)),
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getAuthorId, userId)
                        .eq(Content::getStatus, PUBLISHED)
                        .eq(Content::getIsDeleted, NOT_DELETED)
                        .orderByDesc(Content::getPublishTime)
                        .orderByDesc(Content::getCreateTime));
        List<MyContentVO> records = page.getRecords().stream().map(this::toContent).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<MyFavoriteVO> favorites(Long userId, Long pageNum, Long pageSize, String targetType) {
        requireSelf(userId);
        return userProfileService.myFavorites(pageNum, pageSize, targetType);
    }

    public PageResult<MyCommentVO> comments(Long userId, Long pageNum, Long pageSize, String targetType) {
        requireSelf(userId);
        return userProfileService.myComments(pageNum, pageSize, targetType);
    }

    public UserStandVO stand() {
        requireActiveCurrentUser();
        UserProfileVO profile = userProfileService.me();
        UserStandVO vo = new UserStandVO();
        vo.setUserId(profile.getUserId());
        vo.setUsername(profile.getUsername());
        vo.setNickname(profile.getNickname());
        vo.setAvatarUrl(profile.getAvatarUrl());
        vo.setBio(profile.getBio());
        vo.setMainTeam(profile.getMainTeam());
        vo.setFollowTeams(profile.getFollowTeams());
        vo.setFollowPlayers(profile.getFollowPlayers());
        vo.setFollowingUserCount(countFollowing(profile.getUserId()));
        vo.setFollowerCount(countFollowers(profile.getUserId()));
        vo.setContentCount(countVisibleContents(profile.getUserId()));
        vo.setLikeReceivedCount(countLikeReceived(profile.getUserId()));
        return vo;
    }

    public UserRelationStatus relationStatus(Long viewerId, Long targetUserId) {
        if (viewerId == null) {
            return UserRelationStatus.NONE;
        }
        if (Objects.equals(viewerId, targetUserId)) {
            return UserRelationStatus.SELF;
        }
        boolean viewerFollowsTarget = isActiveFollow(viewerId, targetUserId);
        boolean targetFollowsViewer = isActiveFollow(targetUserId, viewerId);
        if (viewerFollowsTarget && targetFollowsViewer) {
            return UserRelationStatus.MUTUAL;
        }
        if (viewerFollowsTarget) {
            return UserRelationStatus.FOLLOWING;
        }
        if (targetFollowsViewer) {
            return UserRelationStatus.FOLLOWED_BY;
        }
        return UserRelationStatus.NONE;
    }

    private UserPublicProfileVO toPublicProfile(SysUser user, UserProfile profile, Long viewerId) {
        UserPublicProfileVO vo = new UserPublicProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(profile == null ? user.getUsername() : defaultString(profile.getNickname(), user.getUsername()));
        vo.setAvatarUrl(profile == null ? "" : defaultString(profile.getAvatarUrl(), ""));
        vo.setBio(profile == null ? "" : defaultString(profile.getBio(), ""));
        vo.setMainTeam(mainTeam(profile));
        vo.setFollowingCount(countFollowing(user.getId()));
        vo.setFollowerCount(countFollowers(user.getId()));
        vo.setContentCount(countVisibleContents(user.getId()));
        vo.setLikeReceivedCount(countLikeReceived(user.getId()));
        vo.setRelationStatus(relationStatus(viewerId, user.getId()));
        vo.setCurrentUser(Objects.equals(viewerId, user.getId()));
        return vo;
    }

    private List<UserFollowItemVO> toFollowItems(List<FollowRecord> records, boolean targetSide, Long viewerId) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = records.stream()
                .map(record -> targetSide ? record.getTargetId() : record.getUserId())
                .distinct()
                .toList();
        Map<Long, SysUser> users = sysUserMapper.selectBatchIds(userIds).stream()
                .filter(this::isVisibleUser)
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, UserProfile> profiles = profilesOf(userIds);
        List<UserFollowItemVO> result = new ArrayList<>();
        for (FollowRecord record : records) {
            Long relatedUserId = targetSide ? record.getTargetId() : record.getUserId();
            SysUser user = users.get(relatedUserId);
            if (user == null) {
                continue;
            }
            UserProfile profile = profiles.get(relatedUserId);
            UserFollowItemVO vo = new UserFollowItemVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setNickname(profile == null ? user.getUsername() : defaultString(profile.getNickname(), user.getUsername()));
            vo.setAvatarUrl(profile == null ? "" : defaultString(profile.getAvatarUrl(), ""));
            vo.setBio(profile == null ? "" : defaultString(profile.getBio(), ""));
            vo.setRelationStatus(relationStatus(viewerId, user.getId()));
            vo.setFollowedAt(record.getUpdateTime() == null ? record.getCreateTime() : record.getUpdateTime());
            result.add(vo);
        }
        return result;
    }

    private UserFollowActionVO actionResult(Long viewerId, Long targetUserId, boolean followed) {
        UserFollowActionVO vo = new UserFollowActionVO();
        vo.setTargetUserId(targetUserId);
        vo.setFollowed(followed);
        vo.setRelationStatus(relationStatus(viewerId, targetUserId));
        vo.setFollowingCount(countFollowing(viewerId));
        vo.setFollowerCount(countFollowers(targetUserId));
        return vo;
    }

    private void syncUserCounts(Long currentUserId, Long targetUserId) {
        followService.syncUserProfileCounts(currentUserId);
        syncFollowerCount(targetUserId);
    }

    private void syncFollowerCount(Long userId) {
        userProfileMapper.update(null, new UpdateWrapper<UserProfile>()
                .eq("user_id", userId)
                .set("follower_count", countFollowers(userId)));
    }

    private FollowRecord findRecord(Long userId, Long targetUserId) {
        return followRecordMapper.selectOne(new LambdaQueryWrapper<FollowRecord>()
                .eq(FollowRecord::getUserId, userId)
                .eq(FollowRecord::getFollowType, FollowService.TYPE_USER)
                .eq(FollowRecord::getTargetId, targetUserId)
                .last("LIMIT 1"));
    }

    private boolean isActiveFollow(Long userId, Long targetUserId) {
        Long count = followRecordMapper.selectCount(activeFollowQuery()
                .eq(FollowRecord::getUserId, userId)
                .eq(FollowRecord::getTargetId, targetUserId));
        return count != null && count > 0;
    }

    private boolean isActiveFollow(FollowRecord record) {
        return record != null
                && FollowService.STATUS_ACTIVE.equals(record.getStatus())
                && Integer.valueOf(NOT_DELETED).equals(record.getIsDeleted());
    }

    private LambdaQueryWrapper<FollowRecord> activeFollowQuery() {
        return new LambdaQueryWrapper<FollowRecord>()
                .eq(FollowRecord::getFollowType, FollowService.TYPE_USER)
                .eq(FollowRecord::getStatus, FollowService.STATUS_ACTIVE)
                .eq(FollowRecord::getIsDeleted, NOT_DELETED);
    }

    private int countFollowing(Long userId) {
        Long count = followRecordMapper.selectCount(activeFollowQuery().eq(FollowRecord::getUserId, userId));
        return count == null ? 0 : count.intValue();
    }

    private int countFollowers(Long userId) {
        Long count = followRecordMapper.selectCount(activeFollowQuery().eq(FollowRecord::getTargetId, userId));
        return count == null ? 0 : count.intValue();
    }

    private int countVisibleContents(Long userId) {
        Long count = contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getAuthorId, userId)
                .eq(Content::getStatus, PUBLISHED)
                .eq(Content::getIsDeleted, NOT_DELETED));
        return count == null ? 0 : count.intValue();
    }

    private int countLikeReceived(Long userId) {
        List<Content> contents = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .eq(Content::getAuthorId, userId)
                .eq(Content::getStatus, PUBLISHED)
                .eq(Content::getIsDeleted, NOT_DELETED));
        return contents.stream().mapToInt(content -> content.getLikeCount() == null ? 0 : content.getLikeCount()).sum();
    }

    private SysUser requireActiveCurrentUser() {
        Long userId = CurrentUserHolder.get().getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (!isVisibleUser(user)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    private void requireSelf(Long userId) {
        Long currentUserId = CurrentUserHolder.get().getUserId();
        if (!Objects.equals(currentUserId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private SysUser requireVisibleUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (!isVisibleUser(user)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        return user;
    }

    private boolean isVisibleUser(SysUser user) {
        return user != null && ACTIVE.equals(user.getStatus()) && Integer.valueOf(NOT_DELETED).equals(user.getIsDeleted());
    }

    private SysUser requireExistingUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !Integer.valueOf(NOT_DELETED).equals(user.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
        }
        return user;
    }

    private UserProfile profileOf(Long userId) {
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .eq(UserProfile::getIsDeleted, NOT_DELETED)
                .last("LIMIT 1"));
    }

    private Map<Long, UserProfile> profilesOf(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileMapper.selectList(new LambdaQueryWrapper<UserProfile>()
                        .in(UserProfile::getUserId, userIds)
                        .eq(UserProfile::getIsDeleted, NOT_DELETED))
                .stream()
                .collect(Collectors.toMap(UserProfile::getUserId, Function.identity(), (oldValue, newValue) -> oldValue));
    }

    private TeamBriefVO mainTeam(UserProfile profile) {
        if (profile == null || profile.getMainTeamId() == null) {
            return null;
        }
        FootballTeam team = footballTeamMapper.selectById(profile.getMainTeamId());
        if (team == null || !ACTIVE.equals(team.getStatus()) || Integer.valueOf(1).equals(team.getIsDeleted())) {
            return null;
        }
        TeamBriefVO vo = new TeamBriefVO();
        vo.setTeamId(team.getId());
        vo.setTeamName(team.getTeamName());
        vo.setLogoUrl(defaultString(team.getLogoUrl(), ""));
        return vo;
    }

    private MyContentVO toContent(Content content) {
        MyContentVO vo = new MyContentVO();
        vo.setContentId(content.getId());
        vo.setContentType(content.getContentType());
        vo.setTitle(content.getTitle());
        vo.setSummary(defaultString(content.getSummary(), ""));
        vo.setCoverUrl(defaultString(content.getCoverUrl(), ""));
        vo.setStatus(content.getStatus());
        vo.setLikeCount(content.getLikeCount() == null ? 0 : content.getLikeCount());
        vo.setCommentCount(content.getCommentCount() == null ? 0 : content.getCommentCount());
        vo.setFavoriteCount(content.getFavoriteCount() == null ? 0 : content.getFavoriteCount());
        vo.setPublishTime(content.getPublishTime());
        vo.setCreateTime(content.getCreateTime());
        return vo;
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUserContext loginUser)) {
            return null;
        }
        return loginUser.getUserId();
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

    private String defaultString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
