package com.southstand.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.follow.dto.FollowToggleRequest;
import com.southstand.follow.entity.FollowRecord;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.follow.service.FollowService;
import com.southstand.follow.vo.FollowToggleResponse;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FollowServiceTests {

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void toggleTeamFollowAndCancel() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        FootballTeamMapper teamMapper = mock(FootballTeamMapper.class);
        FollowRecordMapper followMapper = mock(FollowRecordMapper.class);
        when(teamMapper.selectById(30001L)).thenReturn(activeTeam());
        when(followMapper.selectCount(any())).thenReturn(1L);

        FollowToggleRequest request = new FollowToggleRequest();
        request.setFollowType(FollowService.TYPE_TEAM);
        request.setTargetId(30001L);

        when(followMapper.selectOne(any())).thenReturn(null);
        FollowToggleResponse followed = service(followMapper, teamMapper).toggle(request);
        assertThat(followed.getFollowed()).isTrue();

        FollowRecord active = new FollowRecord();
        active.setId(80001L);
        active.setStatus(FollowService.STATUS_ACTIVE);
        active.setIsDeleted(0);
        when(followMapper.selectOne(any())).thenReturn(active);
        FollowToggleResponse cancelled = service(followMapper, teamMapper).toggle(request);
        assertThat(cancelled.getFollowed()).isFalse();
    }

    private FollowService service(FollowRecordMapper followMapper, FootballTeamMapper teamMapper) {
        return new FollowService(
                followMapper,
                teamMapper,
                mock(FootballPlayerMapper.class),
                mock(SysUserMapper.class),
                mock(UserProfileMapper.class)
        );
    }

    private FootballTeam activeTeam() {
        FootballTeam team = new FootballTeam();
        team.setId(30001L);
        team.setTeamName("巴塞罗那");
        team.setStatus(FollowService.STATUS_ACTIVE);
        team.setIsDeleted(0);
        return team;
    }
}
