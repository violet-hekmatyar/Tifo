package com.southstand.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.follow.service.FollowService;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.entity.TeamPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.player.mapper.TeamPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.onboarding.dto.SavePreferencesRequest;
import com.southstand.onboarding.service.OnboardingService;
import com.southstand.onboarding.vo.OnboardingOptionsVO;
import com.southstand.onboarding.vo.SavePreferencesResponse;
import com.southstand.user.entity.UserOnboarding;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserOnboardingMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OnboardingServiceTests {

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void optionsReturnsNonEmptyWhenSeedDataExists() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        FootballTeamMapper teamMapper = mock(FootballTeamMapper.class);
        FootballPlayerMapper playerMapper = mock(FootballPlayerMapper.class);
        TeamPlayerMapper teamPlayerMapper = mock(TeamPlayerMapper.class);
        FollowService followService = mock(FollowService.class);

        FootballTeam team = activeTeam(30001L, "巴塞罗那");
        FootballPlayer player = activePlayer(40001L, "莱万多夫斯基");
        TeamPlayer relation = new TeamPlayer();
        relation.setPlayerId(40001L);
        relation.setTeamId(30001L);
        relation.setStatus("ACTIVE");
        relation.setIsDeleted(0);

        when(followService.findActiveTargetIds(10002L, FollowService.TYPE_TEAM)).thenReturn(Set.of());
        when(followService.findActiveTargetIds(10002L, FollowService.TYPE_PLAYER)).thenReturn(Set.of());
        when(teamMapper.selectList(any())).thenReturn(List.of(team));
        when(playerMapper.selectList(any())).thenReturn(List.of(player));
        when(teamPlayerMapper.selectList(any())).thenReturn(List.of(relation));
        when(teamMapper.selectBatchIds(any())).thenReturn(List.of(team));

        OnboardingOptionsVO options = service(teamMapper, playerMapper, teamPlayerMapper, followService).options();

        assertThat(options.getRecommendedTeams()).isNotEmpty();
        assertThat(options.getRecommendedPlayers()).isNotEmpty();
    }

    @Test
    void savePreferencesAcceptsMoreThanFiveTeams() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        FollowService followService = mock(FollowService.class);
        UserOnboardingMapper onboardingMapper = mock(UserOnboardingMapper.class);
        when(followService.requireActiveTeam(any())).thenReturn(activeTeam(30001L, "巴塞罗那"));
        when(followService.requireActivePlayer(any())).thenReturn(activePlayer(40001L, "莱万多夫斯基"));
        when(followService.syncUserProfileCounts(10002L)).thenReturn(new FollowService.FollowCounts(7, 2, 0));
        when(onboardingMapper.selectOne(any())).thenReturn(new UserOnboarding());

        OnboardingService service = new OnboardingService(
                mock(FootballTeamMapper.class),
                mock(FootballPlayerMapper.class),
                mock(TeamPlayerMapper.class),
                mock(UserProfileMapper.class),
                onboardingMapper,
                mock(SysUserMapper.class),
                followService
        );
        SavePreferencesRequest request = new SavePreferencesRequest();
        request.setMainTeamId(30001L);
        request.setFollowTeamIds(List.of(30001L, 30002L, 30003L, 30004L, 30005L, 30006L, 30007L));
        request.setFollowPlayerIds(List.of(40001L, 40002L));

        SavePreferencesResponse response = service.savePreferences(request);

        assertThat(response.getFollowTeamCount()).isEqualTo(7);
        verify(followService, times(7)).ensureActiveFollow(eq(10002L), eq(FollowService.TYPE_TEAM), any(), any(Boolean.class));
    }

    @Test
    void invalidMainTeamReturnsNotFound() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        FollowService followService = mock(FollowService.class);
        when(followService.requireActiveTeam(99999L)).thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

        SavePreferencesRequest request = new SavePreferencesRequest();
        request.setMainTeamId(99999L);

        assertThatThrownBy(() -> service(mock(FootballTeamMapper.class), mock(FootballPlayerMapper.class), mock(TeamPlayerMapper.class), followService)
                .savePreferences(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    private OnboardingService service(
            FootballTeamMapper teamMapper,
            FootballPlayerMapper playerMapper,
            TeamPlayerMapper teamPlayerMapper,
            FollowService followService
    ) {
        return new OnboardingService(
                teamMapper,
                playerMapper,
                teamPlayerMapper,
                mock(UserProfileMapper.class),
                mock(UserOnboardingMapper.class),
                mock(SysUserMapper.class),
                followService
        );
    }

    private FootballTeam activeTeam(Long id, String name) {
        FootballTeam team = new FootballTeam();
        team.setId(id);
        team.setTeamName(name);
        team.setCountry("Spain");
        team.setStatus("ACTIVE");
        team.setIsDeleted(0);
        return team;
    }

    private FootballPlayer activePlayer(Long id, String name) {
        FootballPlayer player = new FootballPlayer();
        player.setId(id);
        player.setPlayerName(name);
        player.setStatus("ACTIVE");
        player.setIsDeleted(0);
        return player;
    }
}
