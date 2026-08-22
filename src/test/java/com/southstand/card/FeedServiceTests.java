package com.southstand.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.southstand.auth.security.JwtAuthenticationToken;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.card.service.FeedService;
import com.southstand.card.vo.FeedPageResult;
import com.southstand.card.vo.HotLeagueVO;
import com.southstand.content.entity.Content;
import com.southstand.content.entity.ContentRelation;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.mapper.ContentRelationMapper;
import com.southstand.follow.entity.FollowRecord;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.football.event.mapper.MatchEventMapper;
import com.southstand.football.league.entity.FootballLeague;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.report.mapper.MatchReportMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class FeedServiceTests {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recommendWithoutLoginReturnsCards() {
        FeedPageResult page = fixture().service().feed("recommend", null, null, 1, 10, null);

        assertThat(page.getRecords()).isNotEmpty();
        assertThat(page.getRecords()).extracting("cardType").contains("CONTENT", "MATCH");
        assertThat(page.getRecords()).allMatch(card -> card.getCardKey() != null && !card.getCardKey().isBlank());
    }

    @Test
    void newsReturnsOnlyContentCards() {
        FeedPageResult page = fixture().service().feed("news", null, null, 1, 10, null);

        assertThat(page.getRecords()).isNotEmpty();
        assertThat(page.getRecords()).allMatch(card -> "CONTENT".equals(card.getCardType()));
        assertThat(page.getRecords()).noneMatch(card -> "POST".equals(card.getContentType()));
    }

    @Test
    void matchTabReturnsMatchCards() {
        FeedPageResult page = fixture().service().feed("match", null, null, 1, 10, null);

        assertThat(page.getRecords()).isNotEmpty();
        assertThat(page.getRecords()).allMatch(card -> "MATCH".equals(card.getCardType()));
    }

    @Test
    void followingAfterLoginReturnsFollowedRelatedCards() {
        login();
        FeedFixture fixture = fixture();
        fixture.followRecords = List.of(follow("TEAM", 30001L), follow("PLAYER", 40001L));
        fixture.profile.setMainTeamId(30001L);

        FeedPageResult page = fixture.service().feed("following", null, null, 1, 10, null);

        assertThat(page.getRecords()).isNotEmpty();
        assertThat(page.getRecords()).anyMatch(card -> "CONTENT".equals(card.getCardType()));
        assertThat(page.getRecords()).anyMatch(card -> "MATCH".equals(card.getCardType()));
    }

    @Test
    void pageSizeIsCappedAt100() {
        FeedFixture fixture = fixture();
        List<Content> contents = new ArrayList<>();
        for (long i = 1; i <= 120; i++) {
            contents.add(content(20000L + i, "NEWS", 10));
        }
        fixture.contents = contents;
        fixture.matches = List.of();

        FeedPageResult page = fixture.service().feed("news", null, null, 1, 500, null);

        assertThat(page.getPageSize()).isEqualTo(100);
        assertThat(page.getRecords()).hasSize(100);
    }

    @Test
    void hotLeaguesReturnActiveLeagueCounts() {
        FeedFixture fixture = fixture();
        fixture.leagues = List.of(league(10001L, "Premier League"), league(10003L, "La Liga"));
        when(fixture.matchInfoMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L, 2L, 0L, 1L);

        List<HotLeagueVO> leagues = fixture.service().hotLeagues(10);

        assertThat(leagues).hasSize(2);
        assertThat(leagues.get(0).getLiveMatchCount()).isEqualTo(1);
        assertThat(leagues.get(0).getUpcomingMatchCount()).isEqualTo(2);
    }

    private FeedFixture fixture() {
        return new FeedFixture();
    }

    private void login() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                new LoginUserContext(10002L, "demo", "USER"),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    private static Content content(Long id, String type, int hotScore) {
        Content content = new Content();
        content.setId(id);
        content.setContentType(type);
        content.setTitle(type + " title " + id);
        content.setSummary("summary");
        content.setCoverUrl("/uploads/content/" + id + ".jpg");
        content.setAuthorId(10001L);
        content.setLikeCount(hotScore);
        content.setCommentCount(2);
        content.setFavoriteCount(1);
        content.setHotScore(BigDecimal.valueOf(hotScore));
        content.setPublishTime(LocalDateTime.now().minusHours(1));
        content.setStatus("PUBLISHED");
        content.setIsDeleted(0);
        return content;
    }

    private static ContentRelation relation(Long contentId, String type, Long relationId) {
        ContentRelation relation = new ContentRelation();
        relation.setId(contentId + relationId);
        relation.setContentId(contentId);
        relation.setRelationType(type);
        relation.setRelationId(relationId);
        relation.setStatus("ACTIVE");
        relation.setIsDeleted(0);
        return relation;
    }

    private static MatchInfo match(Long id, Long leagueId, Long homeTeamId, Long awayTeamId, String status) {
        MatchInfo match = new MatchInfo();
        match.setId(id);
        match.setLeagueId(leagueId);
        match.setHomeTeamId(homeTeamId);
        match.setAwayTeamId(awayTeamId);
        match.setHomeScore(2);
        match.setAwayScore(1);
        match.setMatchStatus(status);
        match.setMatchTime(LocalDateTime.now().plusDays(1));
        match.setImportantLevel(5);
        match.setHasReport(1);
        match.setStatus("ACTIVE");
        match.setIsDeleted(0);
        return match;
    }

    private static FootballLeague league(Long id, String name) {
        FootballLeague league = new FootballLeague();
        league.setId(id);
        league.setLeagueName(name);
        league.setCountry("Demo");
        league.setStatus("ACTIVE");
        league.setIsDeleted(0);
        return league;
    }

    private static FootballTeam team(Long id, String name) {
        FootballTeam team = new FootballTeam();
        team.setId(id);
        team.setTeamName(name);
        team.setLogoUrl("/uploads/team/" + id + ".png");
        team.setStatus("ACTIVE");
        team.setIsDeleted(0);
        return team;
    }

    private static FollowRecord follow(String type, Long targetId) {
        FollowRecord follow = new FollowRecord();
        follow.setFollowType(type);
        follow.setTargetId(targetId);
        follow.setStatus("ACTIVE");
        follow.setIsDeleted(0);
        return follow;
    }

    private class FeedFixture {
        private List<Content> contents = List.of(content(20001L, "NEWS", 20), content(20003L, "ARTICLE", 18));
        private List<MatchInfo> matches = List.of(match(50003L, 10003L, 30002L, 30001L, "LIVE"));
        private List<FootballLeague> leagues = List.of(league(10001L, "Premier League"), league(10003L, "La Liga"));
        private List<FollowRecord> followRecords = List.of();
        private final UserProfile profile = new UserProfile();
        private final ContentMapper contentMapper = mock(ContentMapper.class);
        private final ContentRelationMapper relationMapper = mock(ContentRelationMapper.class);
        private final MatchInfoMapper matchInfoMapper = mock(MatchInfoMapper.class);
        private final FootballLeagueMapper leagueMapper = mock(FootballLeagueMapper.class);
        private final FootballTeamMapper teamMapper = mock(FootballTeamMapper.class);

        private FeedService service() {
            profile.setUserId(10002L);
            profile.setStatus("ACTIVE");
            profile.setIsDeleted(0);
            when(contentMapper.selectList(any(QueryWrapper.class))).thenAnswer(invocation -> contents.stream()
                    .filter(content -> !"POST".equals(content.getContentType()))
                    .toList());
            when(relationMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(relation(20001L, "TEAM", 30001L)));
            when(matchInfoMapper.selectList(any(QueryWrapper.class))).thenReturn(matches);
            when(leagueMapper.selectList(any(QueryWrapper.class))).thenReturn(leagues);
            when(leagueMapper.selectById(10001L)).thenReturn(league(10001L, "Premier League"));
            when(leagueMapper.selectById(10003L)).thenReturn(league(10003L, "La Liga"));
            when(teamMapper.selectById(30001L)).thenReturn(team(30001L, "Barcelona"));
            when(teamMapper.selectById(30002L)).thenReturn(team(30002L, "Real Madrid"));
            UserProfileMapper userProfileMapper = mock(UserProfileMapper.class);
            when(userProfileMapper.selectOne(any(QueryWrapper.class))).thenReturn(profile);
            FollowRecordMapper followMapper = mock(FollowRecordMapper.class);
            when(followMapper.selectList(any(QueryWrapper.class))).thenReturn(followRecords);
            LikeRecordMapper likeMapper = mock(LikeRecordMapper.class);
            FavoriteRecordMapper favoriteMapper = mock(FavoriteRecordMapper.class);
            when(likeMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(favoriteMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            return new FeedService(
                    contentMapper,
                    relationMapper,
                    matchInfoMapper,
                    mock(MatchEventMapper.class),
                    mock(MatchReportMapper.class),
                    leagueMapper,
                    teamMapper,
                    mock(FootballPlayerMapper.class),
                    followMapper,
                    userProfileMapper,
                    likeMapper,
                    favoriteMapper,
                    mock(com.southstand.interaction.mapper.CommentMapper.class)
            );
        }
    }
}
