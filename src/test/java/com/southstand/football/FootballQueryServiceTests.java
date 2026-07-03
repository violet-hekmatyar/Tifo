package com.southstand.football;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.follow.entity.FollowRecord;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.football.event.entity.MatchEvent;
import com.southstand.football.event.mapper.MatchEventMapper;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.match.vo.MatchDetailVO;
import com.southstand.football.match.vo.MatchListVO;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.player.mapper.TeamPlayerMapper;
import com.southstand.football.report.entity.MatchReport;
import com.southstand.football.report.mapper.MatchReportMapper;
import com.southstand.football.schedule.service.FootballQueryService;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class FootballQueryServiceTests {

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void leaguesReturnActiveSeedLikeRecords() {
        FootballLeagueMapper leagueMapper = mock(FootballLeagueMapper.class);
        when(leagueMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(league(10003L, "La Liga")));

        assertThat(service(leagueMapper).leagues())
                .hasSize(1)
                .first()
                .extracting("leagueId", "leagueName")
                .containsExactly(10003L, "La Liga");
    }

    @Test
    void importantMatchesReturnRecords() {
        MatchInfoMapper matchInfoMapper = mock(MatchInfoMapper.class);
        when(matchInfoMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(match(50001L, 10002L, 30001L, 30003L)));

        PageResult<MatchListVO> page = service(matchInfoMapper).importantMatches(null, 1, 10);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getMatchId()).isEqualTo(50001L);
    }

    @Test
    void matchesCanReturnFilteredTeamAndLeagueRecords() {
        MatchInfoMapper matchInfoMapper = mock(MatchInfoMapper.class);
        when(matchInfoMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(match(50003L, 10003L, 30002L, 30001L)));

        PageResult<MatchListVO> page = service(matchInfoMapper).matches(10003L, 30001L, null, null, 1, 10);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getLeagueId()).isEqualTo(10003L);
        assertThat(page.getRecords().get(0).getAwayTeam().getTeamId()).isEqualTo(30001L);
    }

    @Test
    void followingTeamMatchesUseOnlyFollowedTeamIds() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "demo", "USER"));
        FollowRecordMapper followRecordMapper = mock(FollowRecordMapper.class);
        FollowRecord follow = new FollowRecord();
        follow.setTargetId(30001L);
        when(followRecordMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(follow));
        MatchInfoMapper matchInfoMapper = mock(MatchInfoMapper.class);
        when(matchInfoMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(match(50005L, 10003L, 30001L, 30002L)));

        PageResult<MatchListVO> page = service(matchInfoMapper, followRecordMapper).followingTeamMatches(null, 1, 10);

        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getHomeTeam().getTeamId()).isEqualTo(30001L);
    }

    @Test
    void teamPlayerAndMatchNotFoundReturn40401() {
        FootballQueryService service = service();

        assertThatThrownBy(() -> service.teamDetail(99999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> service.playerDetail(99999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> service.matchDetail(99999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void matchDetailContainsEventsAndReportEntry() {
        MatchInfoMapper matchInfoMapper = mock(MatchInfoMapper.class);
        MatchEventMapper eventMapper = mock(MatchEventMapper.class);
        MatchReportMapper reportMapper = mock(MatchReportMapper.class);
        ContentMapper contentMapper = mock(ContentMapper.class);
        when(matchInfoMapper.selectById(50001L)).thenReturn(match(50001L, 10002L, 30001L, 30003L));
        when(eventMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(goalEvent()));
        MatchReport report = new MatchReport();
        report.setContentId(20004L);
        report.setReportType("REPORT");
        when(reportMapper.selectOne(any(QueryWrapper.class))).thenReturn(report);
        Content content = new Content();
        content.setId(20004L);
        content.setTitle("Barcelona 2-1 Bayern demo report");
        when(contentMapper.selectById(20004L)).thenReturn(content);

        MatchDetailVO detail = service(matchInfoMapper, eventMapper, reportMapper, contentMapper).matchDetail(50001L);

        assertThat(detail.getEventList()).hasSize(1);
        assertThat(detail.getEventList().get(0).getEventType()).isEqualTo("GOAL");
        assertThat(detail.getReport().getContentId()).isEqualTo(20004L);
        assertThat(detail.getReport().getTitle()).isEqualTo("Barcelona 2-1 Bayern demo report");
    }

    private FootballQueryService service() {
        return service(mock(FootballLeagueMapper.class));
    }

    private FootballQueryService service(FootballLeagueMapper leagueMapper) {
        return service(leagueMapper, mock(MatchInfoMapper.class), mock(MatchEventMapper.class), mock(MatchReportMapper.class),
                mock(ContentMapper.class), mock(FollowRecordMapper.class));
    }

    private FootballQueryService service(MatchInfoMapper matchInfoMapper) {
        return service(mock(FootballLeagueMapper.class), matchInfoMapper, mock(MatchEventMapper.class), mock(MatchReportMapper.class),
                mock(ContentMapper.class), mock(FollowRecordMapper.class));
    }

    private FootballQueryService service(MatchInfoMapper matchInfoMapper, FollowRecordMapper followRecordMapper) {
        return service(mock(FootballLeagueMapper.class), matchInfoMapper, mock(MatchEventMapper.class), mock(MatchReportMapper.class),
                mock(ContentMapper.class), followRecordMapper);
    }

    private FootballQueryService service(
            MatchInfoMapper matchInfoMapper,
            MatchEventMapper eventMapper,
            MatchReportMapper reportMapper,
            ContentMapper contentMapper
    ) {
        return service(mock(FootballLeagueMapper.class), matchInfoMapper, eventMapper, reportMapper, contentMapper, mock(FollowRecordMapper.class));
    }

    private FootballQueryService service(
            FootballLeagueMapper leagueMapper,
            MatchInfoMapper matchInfoMapper,
            MatchEventMapper eventMapper,
            MatchReportMapper reportMapper,
            ContentMapper contentMapper,
            FollowRecordMapper followRecordMapper
    ) {
        FootballTeamMapper teamMapper = mock(FootballTeamMapper.class);
        when(teamMapper.selectById(30001L)).thenReturn(team(30001L, "Barcelona"));
        when(teamMapper.selectById(30002L)).thenReturn(team(30002L, "Real Madrid"));
        when(teamMapper.selectById(30003L)).thenReturn(team(30003L, "Bayern Munich"));
        FootballPlayerMapper playerMapper = mock(FootballPlayerMapper.class);
        when(playerMapper.selectById(40001L)).thenReturn(player(40001L, "Robert Lewandowski"));
        return new FootballQueryService(
                leagueMapper,
                teamMapper,
                playerMapper,
                mock(TeamPlayerMapper.class),
                matchInfoMapper,
                eventMapper,
                reportMapper,
                contentMapper,
                followRecordMapper
        );
    }

    private FootballLeague league(Long id, String name) {
        FootballLeague league = new FootballLeague();
        league.setId(id);
        league.setLeagueName(name);
        league.setLeagueNameEn(name);
        league.setCountry("Demo");
        league.setSeason("2026");
        league.setLeagueType("LEAGUE");
        league.setStatus("ACTIVE");
        league.setIsDeleted(0);
        return league;
    }

    private MatchInfo match(Long id, Long leagueId, Long homeTeamId, Long awayTeamId) {
        MatchInfo match = new MatchInfo();
        match.setId(id);
        match.setLeagueId(leagueId);
        match.setHomeTeamId(homeTeamId);
        match.setAwayTeamId(awayTeamId);
        match.setHomeScore(2);
        match.setAwayScore(1);
        match.setMatchTime(LocalDateTime.of(2026, 7, 10, 20, 0));
        match.setMatchStatus("FINISHED");
        match.setImportantLevel(5);
        match.setHasReport(1);
        match.setStatus("ACTIVE");
        match.setIsDeleted(0);
        return match;
    }

    private MatchEvent goalEvent() {
        MatchEvent event = new MatchEvent();
        event.setId(51001L);
        event.setMatchId(50001L);
        event.setTeamId(30001L);
        event.setPlayerId(40001L);
        event.setEventType("GOAL");
        event.setMinute(63);
        event.setScoreAfter("2-1");
        event.setHasDebate(0);
        return event;
    }

    private FootballTeam team(Long id, String name) {
        FootballTeam team = new FootballTeam();
        team.setId(id);
        team.setTeamName(name);
        team.setLogoUrl("/uploads/team/" + id + ".png");
        team.setStatus("ACTIVE");
        team.setIsDeleted(0);
        return team;
    }

    private FootballPlayer player(Long id, String name) {
        FootballPlayer player = new FootballPlayer();
        player.setId(id);
        player.setPlayerName(name);
        player.setStatus("ACTIVE");
        player.setIsDeleted(0);
        return player;
    }
}
