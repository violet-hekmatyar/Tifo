package com.southstand.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.follow.entity.FollowRecord;
import com.southstand.follow.service.FollowService;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.entity.TeamPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.player.mapper.TeamPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.entity.FavoriteRecord;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.user.dto.UpdateMyProfileRequest;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import com.southstand.user.vo.FollowStatsVO;
import com.southstand.user.vo.MyCommentVO;
import com.southstand.user.vo.MyContentVO;
import com.southstand.user.vo.MyFavoriteVO;
import com.southstand.user.vo.MyProfileUpdateVO;
import com.southstand.user.vo.MyStatsVO;
import com.southstand.user.vo.PlayerBriefVO;
import com.southstand.user.vo.TeamBriefVO;
import com.southstand.user.vo.UserProfileVO;
import com.southstand.user.vo.UserSummaryVO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final FootballTeamMapper footballTeamMapper;
    private final FootballPlayerMapper footballPlayerMapper;
    private final TeamPlayerMapper teamPlayerMapper;
    private final FollowService followService;
    private final ContentMapper contentMapper;
    private final FavoriteRecordMapper favoriteRecordMapper;
    private final CommentMapper commentMapper;

    public UserProfileService(
            SysUserMapper sysUserMapper,
            UserProfileMapper userProfileMapper,
            FootballTeamMapper footballTeamMapper,
            FootballPlayerMapper footballPlayerMapper,
            TeamPlayerMapper teamPlayerMapper,
            FollowService followService,
            ContentMapper contentMapper,
            FavoriteRecordMapper favoriteRecordMapper,
            CommentMapper commentMapper
    ) {
        this.sysUserMapper = sysUserMapper;
        this.userProfileMapper = userProfileMapper;
        this.footballTeamMapper = footballTeamMapper;
        this.footballPlayerMapper = footballPlayerMapper;
        this.teamPlayerMapper = teamPlayerMapper;
        this.followService = followService;
        this.contentMapper = contentMapper;
        this.favoriteRecordMapper = favoriteRecordMapper;
        this.commentMapper = commentMapper;
    }

    public UserProfileVO me() {
        SysUser user = requireActiveCurrentUser();
        Long userId = user.getId();
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .last("LIMIT 1"));
        FollowService.FollowCounts counts = followService.syncUserProfileCounts(userId);

        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(profile == null ? user.getUsername() : defaultString(profile.getNickname(), user.getUsername()));
        vo.setAvatarUrl(profile == null ? "" : defaultString(profile.getAvatarUrl(), ""));
        vo.setBio(profile == null ? "" : defaultString(profile.getBio(), ""));
        vo.setRoleType(user.getRoleType());
        vo.setStatus(user.getStatus());
        vo.setOnboardingCompleted(Integer.valueOf(1).equals(user.getOnboardingCompleted()));
        vo.setFollowStats(toStats(profile, counts));

        if (profile != null && profile.getMainTeamId() != null) {
            FootballTeam mainTeam = footballTeamMapper.selectById(profile.getMainTeamId());
            if (isActiveTeam(mainTeam)) {
                vo.setMainTeam(toTeamBrief(mainTeam));
            }
        }
        vo.setFollowTeams(loadFollowTeams(userId));
        vo.setFollowPlayers(loadFollowPlayers(userId));
        return vo;
    }

    public UserSummaryVO summary() {
        SysUser user = requireActiveCurrentUser();
        Long userId = user.getId();
        UserProfile profile = profileOf(userId);
        FollowService.FollowCounts counts = followService.syncUserProfileCounts(userId);

        UserSummaryVO vo = new UserSummaryVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(profile == null ? user.getUsername() : defaultString(profile.getNickname(), user.getUsername()));
        vo.setAvatarUrl(profile == null ? "" : defaultString(profile.getAvatarUrl(), ""));
        vo.setBio(profile == null ? "" : defaultString(profile.getBio(), ""));
        vo.setRoleType(user.getRoleType());
        vo.setStatus(user.getStatus());
        vo.setOnboardingCompleted(Integer.valueOf(1).equals(user.getOnboardingCompleted()));
        if (profile != null && profile.getMainTeamId() != null) {
            FootballTeam mainTeam = footballTeamMapper.selectById(profile.getMainTeamId());
            if (isActiveTeam(mainTeam)) {
                vo.setMainTeam(toTeamBrief(mainTeam));
            }
        }
        vo.setStats(toMyStats(userId, profile, counts));
        vo.setRecentContents(myContents(1L, 3L, null).getRecords());
        vo.setRecentFavorites(myFavorites(1L, 3L, "CONTENT").getRecords());
        vo.setRecentComments(myComments(1L, 3L, "CONTENT").getRecords());
        return vo;
    }

    public MyProfileUpdateVO updateProfile(UpdateMyProfileRequest request) {
        SysUser user = requireActiveCurrentUser();
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        String nickname = trimToNull(request.getNickname());
        if (request.getNickname() != null && (nickname == null || nickname.length() > 30)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "nickname length must be 1-30");
        }
        String bio = request.getBio() == null ? null : request.getBio().trim();
        if (bio != null && bio.length() > 200) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "bio length must be 0-200");
        }
        if (request.getMainTeamId() != null) {
            FootballTeam team = footballTeamMapper.selectById(request.getMainTeamId());
            if (!isActiveTeam(team)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "team not found");
            }
        }

        UserProfile profile = profileOf(user.getId());
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(user.getId());
            profile.setNickname(defaultString(nickname, user.getUsername()));
            profile.setAvatarUrl(defaultString(request.getAvatarUrl(), ""));
            profile.setBio(defaultString(bio, ""));
            profile.setMainTeamId(request.getMainTeamId());
            profile.setPostCount(0);
            profile.setFollowerCount(0);
            profile.setFollowingCount(0);
            profile.setTeamFollowCount(0);
            profile.setPlayerFollowCount(0);
            profile.setStatus("ACTIVE");
            profile.setIsDeleted(0);
            userProfileMapper.insert(profile);
        } else {
            LambdaUpdateWrapper<UserProfile> update = new LambdaUpdateWrapper<UserProfile>()
                    .eq(UserProfile::getUserId, user.getId());
            if (request.getNickname() != null) {
                update.set(UserProfile::getNickname, nickname);
                profile.setNickname(nickname);
            }
            if (request.getAvatarUrl() != null) {
                update.set(UserProfile::getAvatarUrl, request.getAvatarUrl());
                profile.setAvatarUrl(request.getAvatarUrl());
            }
            if (request.getBio() != null) {
                update.set(UserProfile::getBio, bio);
                profile.setBio(bio);
            }
            if (request.getMainTeamId() != null) {
                update.set(UserProfile::getMainTeamId, request.getMainTeamId());
                profile.setMainTeamId(request.getMainTeamId());
            }
            userProfileMapper.update(null, update);
        }

        MyProfileUpdateVO vo = new MyProfileUpdateVO();
        vo.setUserId(user.getId());
        vo.setNickname(profile.getNickname());
        vo.setAvatarUrl(defaultString(profile.getAvatarUrl(), ""));
        vo.setBio(defaultString(profile.getBio(), ""));
        vo.setMainTeamId(profile.getMainTeamId());
        return vo;
    }

    public PageResult<MyContentVO> myContents(Long pageNum, Long pageSize, String contentType) {
        SysUser user = requireActiveCurrentUser();
        Page<Content> page = contentMapper.selectPage(new Page<>(safePageNum(pageNum), safePageSize(pageSize)),
                new LambdaQueryWrapper<Content>()
                        .eq(Content::getAuthorId, user.getId())
                        .eq(Content::getIsDeleted, 0)
                        .eq(!isBlank(contentType), Content::getContentType, contentType)
                        .orderByDesc(Content::getPublishTime)
                        .orderByDesc(Content::getCreateTime));
        List<MyContentVO> records = page.getRecords().stream().map(this::toMyContent).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<MyFavoriteVO> myFavorites(Long pageNum, Long pageSize, String targetType) {
        SysUser user = requireActiveCurrentUser();
        String resolvedTargetType = isBlank(targetType) ? "CONTENT" : targetType.trim();
        if (!"CONTENT".equals(resolvedTargetType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported targetType");
        }
        Page<FavoriteRecord> page = favoriteRecordMapper.selectPage(new Page<>(safePageNum(pageNum), safePageSize(pageSize)),
                new LambdaQueryWrapper<FavoriteRecord>()
                        .eq(FavoriteRecord::getUserId, user.getId())
                        .eq(FavoriteRecord::getTargetType, resolvedTargetType)
                        .eq(FavoriteRecord::getStatus, "ACTIVE")
                        .orderByDesc(FavoriteRecord::getUpdateTime)
                        .orderByDesc(FavoriteRecord::getCreateTime));
        List<MyFavoriteVO> records = new ArrayList<>();
        for (FavoriteRecord record : page.getRecords()) {
            Content content = contentMapper.selectById(record.getTargetId());
            if (isVisibleContent(content)) {
                records.add(toMyFavorite(record, content));
            }
        }
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<MyCommentVO> myComments(Long pageNum, Long pageSize, String targetType) {
        SysUser user = requireActiveCurrentUser();
        Page<Comment> page = commentMapper.selectPage(new Page<>(safePageNum(pageNum), safePageSize(pageSize)),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, user.getId())
                        .eq(Comment::getStatus, "ACTIVE")
                        .eq(Comment::getIsDeleted, 0)
                        .eq(!isBlank(targetType), Comment::getTargetType, targetType)
                        .orderByDesc(Comment::getCreateTime));
        List<MyCommentVO> records = page.getRecords().stream().map(this::toMyComment).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private SysUser requireActiveCurrentUser() {
        Long userId = CurrentUserHolder.get().getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted()) || !"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    private UserProfile profileOf(Long userId) {
        return userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .eq(UserProfile::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    private List<TeamBriefVO> loadFollowTeams(Long userId) {
        List<FollowRecord> records = followService.findActiveRecords(userId, FollowService.TYPE_TEAM);
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> teamIds = records.stream().map(FollowRecord::getTargetId).toList();
        Map<Long, FootballTeam> teams = footballTeamMapper.selectBatchIds(teamIds).stream()
                .filter(this::isActiveTeam)
                .collect(Collectors.toMap(FootballTeam::getId, Function.identity()));
        List<TeamBriefVO> result = new ArrayList<>();
        for (Long teamId : teamIds) {
            FootballTeam team = teams.get(teamId);
            if (team != null) {
                result.add(toTeamBrief(team));
            }
        }
        return result;
    }

    private List<PlayerBriefVO> loadFollowPlayers(Long userId) {
        List<FollowRecord> records = followService.findActiveRecords(userId, FollowService.TYPE_PLAYER);
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> playerIds = records.stream().map(FollowRecord::getTargetId).toList();
        Map<Long, FootballPlayer> players = footballPlayerMapper.selectBatchIds(playerIds).stream()
                .filter(this::isActivePlayer)
                .collect(Collectors.toMap(FootballPlayer::getId, Function.identity()));
        Map<Long, FootballTeam> teamsByPlayerId = loadTeamsByPlayerId(playerIds);

        List<PlayerBriefVO> result = new ArrayList<>();
        for (Long playerId : playerIds) {
            FootballPlayer player = players.get(playerId);
            if (player != null) {
                result.add(toPlayerBrief(player, teamsByPlayerId.get(playerId)));
            }
        }
        return result;
    }

    private Map<Long, FootballTeam> loadTeamsByPlayerId(List<Long> playerIds) {
        List<TeamPlayer> relations = teamPlayerMapper.selectList(new LambdaQueryWrapper<TeamPlayer>()
                .in(TeamPlayer::getPlayerId, playerIds)
                .eq(TeamPlayer::getStatus, FollowService.STATUS_ACTIVE)
                .eq(TeamPlayer::getIsDeleted, 0)
                .orderByAsc(TeamPlayer::getId));
        Map<Long, Long> teamIdByPlayerId = new HashMap<>();
        for (TeamPlayer relation : relations) {
            teamIdByPlayerId.putIfAbsent(relation.getPlayerId(), relation.getTeamId());
        }
        if (teamIdByPlayerId.isEmpty()) {
            return Map.of();
        }
        Map<Long, FootballTeam> teamById = footballTeamMapper.selectBatchIds(teamIdByPlayerId.values()).stream()
                .filter(this::isActiveTeam)
                .collect(Collectors.toMap(FootballTeam::getId, Function.identity()));
        Map<Long, FootballTeam> result = new HashMap<>();
        for (Map.Entry<Long, Long> entry : teamIdByPlayerId.entrySet()) {
            FootballTeam team = teamById.get(entry.getValue());
            if (team != null) {
                result.put(entry.getKey(), team);
            }
        }
        return result;
    }

    private FollowStatsVO toStats(UserProfile profile, FollowService.FollowCounts counts) {
        FollowStatsVO vo = new FollowStatsVO();
        vo.setFollowingCount(counts.getFollowingCount());
        vo.setTeamFollowCount(counts.getTeamFollowCount());
        vo.setPlayerFollowCount(counts.getPlayerFollowCount());
        vo.setFollowerCount(profile == null || profile.getFollowerCount() == null ? 0 : profile.getFollowerCount());
        return vo;
    }

    private MyStatsVO toMyStats(Long userId, UserProfile profile, FollowService.FollowCounts counts) {
        MyStatsVO vo = new MyStatsVO();
        long postCount = contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getAuthorId, userId)
                .eq(Content::getIsDeleted, 0));
        long favoriteCount = favoriteRecordMapper.selectCount(new LambdaQueryWrapper<FavoriteRecord>()
                .eq(FavoriteRecord::getUserId, userId)
                .eq(FavoriteRecord::getTargetType, "CONTENT")
                .eq(FavoriteRecord::getStatus, "ACTIVE"));
        long commentCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId)
                .eq(Comment::getStatus, "ACTIVE")
                .eq(Comment::getIsDeleted, 0));
        vo.setPostCount((int) postCount);
        vo.setFavoriteCount((int) favoriteCount);
        vo.setCommentCount((int) commentCount);
        vo.setFollowingCount(counts.getFollowingCount());
        vo.setTeamFollowCount(counts.getTeamFollowCount());
        vo.setPlayerFollowCount(counts.getPlayerFollowCount());
        vo.setFollowerCount(profile == null || profile.getFollowerCount() == null ? 0 : profile.getFollowerCount());
        return vo;
    }

    private MyContentVO toMyContent(Content content) {
        MyContentVO vo = new MyContentVO();
        vo.setContentId(content.getId());
        vo.setContentType(content.getContentType());
        vo.setTitle(content.getTitle());
        vo.setSummary(defaultString(content.getSummary(), ""));
        vo.setCoverUrl(defaultString(content.getCoverUrl(), ""));
        vo.setStatus(content.getStatus());
        vo.setLikeCount(nvl(content.getLikeCount()));
        vo.setCommentCount(nvl(content.getCommentCount()));
        vo.setFavoriteCount(nvl(content.getFavoriteCount()));
        vo.setPublishTime(content.getPublishTime());
        vo.setCreateTime(content.getCreateTime());
        return vo;
    }

    private MyFavoriteVO toMyFavorite(FavoriteRecord record, Content content) {
        MyFavoriteVO vo = new MyFavoriteVO();
        vo.setFavoriteId(record.getId());
        vo.setTargetType(record.getTargetType());
        vo.setTargetId(record.getTargetId());
        vo.setContentType(content.getContentType());
        vo.setTitle(content.getTitle());
        vo.setSummary(defaultString(content.getSummary(), ""));
        vo.setCoverUrl(defaultString(content.getCoverUrl(), ""));
        vo.setContentStatus(content.getStatus());
        vo.setFavoriteTime(record.getUpdateTime() == null ? record.getCreateTime() : record.getUpdateTime());
        vo.setPublishTime(content.getPublishTime());
        return vo;
    }

    private MyCommentVO toMyComment(Comment comment) {
        MyCommentVO vo = new MyCommentVO();
        vo.setCommentId(comment.getId());
        vo.setTargetType(comment.getTargetType());
        vo.setTargetId(comment.getTargetId());
        vo.setTargetTitle(resolveTargetTitle(comment.getTargetType(), comment.getTargetId()));
        vo.setParentId(comment.getParentId());
        vo.setRootId(comment.getRootId() == null && (comment.getParentId() == null || comment.getParentId() == 0) ? comment.getId() : comment.getRootId());
        vo.setReplyToUserId(comment.getReplyToUserId());
        vo.setReplyToNickname(replyToNickname(comment.getReplyToUserId()));
        vo.setContentText(comment.getContentText());
        vo.setLikeCount(nvl(comment.getLikeCount()));
        vo.setReplyCount(nvl(comment.getReplyCount()));
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }

    private String resolveTargetTitle(String targetType, Long targetId) {
        if (!"CONTENT".equals(targetType)) {
            return null;
        }
        Content content = contentMapper.selectById(targetId);
        return content == null ? null : content.getTitle();
    }

    private String replyToNickname(Long userId) {
        if (userId == null) {
            return null;
        }
        UserProfile profile = profileOf(userId);
        if (profile != null && profile.getNickname() != null) {
            return profile.getNickname();
        }
        SysUser user = sysUserMapper.selectById(userId);
        return user == null ? null : user.getUsername();
    }

    private TeamBriefVO toTeamBrief(FootballTeam team) {
        TeamBriefVO vo = new TeamBriefVO();
        vo.setTeamId(team.getId());
        vo.setTeamName(team.getTeamName());
        vo.setLogoUrl(defaultString(team.getLogoUrl(), ""));
        return vo;
    }

    private PlayerBriefVO toPlayerBrief(FootballPlayer player, FootballTeam team) {
        PlayerBriefVO vo = new PlayerBriefVO();
        vo.setPlayerId(player.getId());
        vo.setPlayerName(player.getPlayerName());
        vo.setAvatarUrl(defaultString(player.getAvatarUrl(), ""));
        vo.setPosition(defaultString(player.getPosition(), ""));
        if (team != null) {
            vo.setTeamId(team.getId());
            vo.setTeamName(team.getTeamName());
        }
        return vo;
    }

    private boolean isActiveTeam(FootballTeam team) {
        return team != null && FollowService.STATUS_ACTIVE.equals(team.getStatus()) && Integer.valueOf(0).equals(team.getIsDeleted());
    }

    private boolean isActivePlayer(FootballPlayer player) {
        return player != null && FollowService.STATUS_ACTIVE.equals(player.getStatus()) && Integer.valueOf(0).equals(player.getIsDeleted());
    }

    private boolean isVisibleContent(Content content) {
        return content != null && Integer.valueOf(0).equals(content.getIsDeleted())
                && ("PUBLISHED".equals(content.getStatus()) || "ACTIVE".equals(content.getStatus()));
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
