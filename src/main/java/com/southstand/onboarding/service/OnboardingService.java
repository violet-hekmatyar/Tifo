package com.southstand.onboarding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.follow.service.FollowService;
import com.southstand.follow.service.FollowService.FollowCounts;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.entity.TeamPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.player.mapper.TeamPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.onboarding.dto.SavePreferencesRequest;
import com.southstand.onboarding.vo.OnboardingOptionsVO;
import com.southstand.onboarding.vo.PlayerOptionVO;
import com.southstand.onboarding.vo.SavePreferencesResponse;
import com.southstand.onboarding.vo.TeamOptionVO;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserOnboarding;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserOnboardingMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {

    private static final int OPTION_LIMIT = 6;

    private final FootballTeamMapper footballTeamMapper;
    private final FootballPlayerMapper footballPlayerMapper;
    private final TeamPlayerMapper teamPlayerMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserOnboardingMapper userOnboardingMapper;
    private final SysUserMapper sysUserMapper;
    private final FollowService followService;

    public OnboardingService(
            FootballTeamMapper footballTeamMapper,
            FootballPlayerMapper footballPlayerMapper,
            TeamPlayerMapper teamPlayerMapper,
            UserProfileMapper userProfileMapper,
            UserOnboardingMapper userOnboardingMapper,
            SysUserMapper sysUserMapper,
            FollowService followService
    ) {
        this.footballTeamMapper = footballTeamMapper;
        this.footballPlayerMapper = footballPlayerMapper;
        this.teamPlayerMapper = teamPlayerMapper;
        this.userProfileMapper = userProfileMapper;
        this.userOnboardingMapper = userOnboardingMapper;
        this.sysUserMapper = sysUserMapper;
        this.followService = followService;
    }

    public OnboardingOptionsVO options() {
        Long userId = CurrentUserHolder.get().getUserId();
        Set<Long> followedTeams = followService.findActiveTargetIds(userId, FollowService.TYPE_TEAM);
        Set<Long> followedPlayers = followService.findActiveTargetIds(userId, FollowService.TYPE_PLAYER);

        List<FootballTeam> teams = footballTeamMapper.selectList(new LambdaQueryWrapper<FootballTeam>()
                .eq(FootballTeam::getStatus, FollowService.STATUS_ACTIVE)
                .eq(FootballTeam::getIsDeleted, 0)
                .orderByDesc(FootballTeam::getFollowerCount)
                .orderByAsc(FootballTeam::getId)
                .last("LIMIT " + OPTION_LIMIT));
        List<FootballPlayer> players = footballPlayerMapper.selectList(new LambdaQueryWrapper<FootballPlayer>()
                .eq(FootballPlayer::getStatus, FollowService.STATUS_ACTIVE)
                .eq(FootballPlayer::getIsDeleted, 0)
                .orderByDesc(FootballPlayer::getFollowerCount)
                .orderByAsc(FootballPlayer::getId)
                .last("LIMIT " + OPTION_LIMIT));

        Map<Long, FootballTeam> teamMap = teams.stream().collect(Collectors.toMap(FootballTeam::getId, Function.identity()));
        teamMap.putAll(loadPlayerTeamMap(players));

        List<TeamOptionVO> teamOptions = teams.stream()
                .map(team -> toTeamOption(team, followedTeams.contains(team.getId())))
                .toList();
        List<PlayerOptionVO> playerOptions = players.stream()
                .map(player -> toPlayerOption(player, teamMap, followedPlayers.contains(player.getId())))
                .toList();

        OnboardingOptionsVO vo = new OnboardingOptionsVO();
        vo.setRecommendedTeams(teamOptions);
        vo.setHotTeams(teamOptions);
        vo.setRecommendedPlayers(playerOptions);
        vo.setHotPlayers(playerOptions);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public SavePreferencesResponse savePreferences(SavePreferencesRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        followService.requireActiveTeam(request.getMainTeamId());

        LinkedHashSet<Long> teamIds = new LinkedHashSet<>();
        teamIds.add(request.getMainTeamId());
        if (request.getFollowTeamIds() != null) {
            teamIds.addAll(request.getFollowTeamIds());
        }

        LinkedHashSet<Long> playerIds = new LinkedHashSet<>();
        if (request.getFollowPlayerIds() != null) {
            playerIds.addAll(request.getFollowPlayerIds());
        }

        for (Long teamId : teamIds) {
            followService.requireActiveTeam(teamId);
            followService.ensureActiveFollow(userId, FollowService.TYPE_TEAM, teamId, request.getMainTeamId().equals(teamId));
        }
        for (Long playerId : playerIds) {
            followService.requireActivePlayer(playerId);
            followService.ensureActiveFollow(userId, FollowService.TYPE_PLAYER, playerId, false);
        }

        userProfileMapper.update(null, new UpdateWrapper<UserProfile>()
                .eq("user_id", userId)
                .set("main_team_id", request.getMainTeamId()));
        sysUserMapper.update(null, new UpdateWrapper<SysUser>()
                .eq("id", userId)
                .set("onboarding_completed", 1));

        UserOnboarding onboarding = userOnboardingMapper.selectOne(new LambdaQueryWrapper<UserOnboarding>()
                .eq(UserOnboarding::getUserId, userId)
                .last("LIMIT 1"));
        if (onboarding == null) {
            onboarding = new UserOnboarding();
            onboarding.setUserId(userId);
            onboarding.setStatus(FollowService.STATUS_ACTIVE);
            onboarding.setIsDeleted(0);
            userOnboardingMapper.insert(onboarding);
        }
        userOnboardingMapper.update(null, new UpdateWrapper<UserOnboarding>()
                .eq("user_id", userId)
                .set("main_team_id", request.getMainTeamId())
                .set("selected_team_ids", toJsonArray(teamIds))
                .set("selected_player_ids", toJsonArray(playerIds))
                .set("completed", 1)
                .set("completed_time", LocalDateTime.now()));

        FollowCounts counts = followService.syncUserProfileCounts(userId);

        SavePreferencesResponse response = new SavePreferencesResponse();
        response.setCompleted(true);
        response.setMainTeamId(request.getMainTeamId());
        response.setFollowTeamCount(counts.getTeamFollowCount());
        response.setFollowPlayerCount(counts.getPlayerFollowCount());
        return response;
    }

    private Map<Long, FootballTeam> loadPlayerTeamMap(List<FootballPlayer> players) {
        if (players.isEmpty()) {
            return Map.of();
        }
        List<Long> playerIds = players.stream().map(FootballPlayer::getId).toList();
        List<TeamPlayer> relations = teamPlayerMapper.selectList(new LambdaQueryWrapper<TeamPlayer>()
                .in(TeamPlayer::getPlayerId, playerIds)
                .eq(TeamPlayer::getStatus, FollowService.STATUS_ACTIVE)
                .eq(TeamPlayer::getIsDeleted, 0)
                .orderByAsc(TeamPlayer::getId));
        Map<Long, Long> playerTeamIds = relations.stream()
                .collect(Collectors.toMap(TeamPlayer::getPlayerId, TeamPlayer::getTeamId, (first, second) -> first));
        if (playerTeamIds.isEmpty()) {
            return Map.of();
        }
        List<FootballTeam> teams = footballTeamMapper.selectBatchIds(new ArrayList<>(playerTeamIds.values()));
        Map<Long, FootballTeam> teamsById = teams.stream().collect(Collectors.toMap(FootballTeam::getId, Function.identity()));
        return playerTeamIds.entrySet().stream()
                .filter(entry -> teamsById.containsKey(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> teamsById.get(entry.getValue())));
    }

    private TeamOptionVO toTeamOption(FootballTeam team, boolean followed) {
        TeamOptionVO vo = new TeamOptionVO();
        vo.setTeamId(team.getId());
        vo.setTeamName(team.getTeamName());
        vo.setLogoUrl(team.getLogoUrl());
        vo.setLeagueName(guessLeagueName(team));
        vo.setCountry(team.getCountry());
        vo.setFollowed(followed);
        return vo;
    }

    private PlayerOptionVO toPlayerOption(FootballPlayer player, Map<Long, FootballTeam> teamMap, boolean followed) {
        FootballTeam team = teamMap.get(player.getId());
        PlayerOptionVO vo = new PlayerOptionVO();
        vo.setPlayerId(player.getId());
        vo.setPlayerName(player.getPlayerName());
        vo.setAvatarUrl(player.getAvatarUrl());
        vo.setPosition(player.getPosition());
        vo.setFollowed(followed);
        if (team != null) {
            vo.setTeamId(team.getId());
            vo.setTeamName(team.getTeamName());
        }
        return vo;
    }

    private String guessLeagueName(FootballTeam team) {
        if ("Spain".equals(team.getCountry())) {
            return "西甲";
        }
        if ("England".equals(team.getCountry())) {
            return "英超";
        }
        if ("Germany".equals(team.getCountry())) {
            return "德甲";
        }
        return "";
    }

    private String toJsonArray(Set<Long> ids) {
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }
}
