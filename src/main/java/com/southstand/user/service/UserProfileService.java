package com.southstand.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.follow.entity.FollowRecord;
import com.southstand.follow.service.FollowService;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.entity.TeamPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.player.mapper.TeamPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import com.southstand.user.vo.FollowStatsVO;
import com.southstand.user.vo.PlayerBriefVO;
import com.southstand.user.vo.TeamBriefVO;
import com.southstand.user.vo.UserProfileVO;
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

    public UserProfileService(
            SysUserMapper sysUserMapper,
            UserProfileMapper userProfileMapper,
            FootballTeamMapper footballTeamMapper,
            FootballPlayerMapper footballPlayerMapper,
            TeamPlayerMapper teamPlayerMapper,
            FollowService followService
    ) {
        this.sysUserMapper = sysUserMapper;
        this.userProfileMapper = userProfileMapper;
        this.footballTeamMapper = footballTeamMapper;
        this.footballPlayerMapper = footballPlayerMapper;
        this.teamPlayerMapper = teamPlayerMapper;
        this.followService = followService;
    }

    public UserProfileVO me() {
        Long userId = CurrentUserHolder.get().getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getIsDeleted())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
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

    private String defaultString(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }
}
