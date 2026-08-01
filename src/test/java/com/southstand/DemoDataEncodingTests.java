package com.southstand;

import static org.assertj.core.api.Assertions.assertThat;

import com.southstand.card.service.FeedService;
import com.southstand.card.vo.FeedCardVO;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.search.service.SearchService;
import com.southstand.search.vo.SearchEntityVO;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoDataEncodingTests {

    private static final long USER_ID = 11000000000000001L;
    private static final long TEAM_ID = 13000000000000001L;
    private static final long PLAYER_ID = 14000000000000001L;
    private static final long CONTENT_ID = 16000000000000001L;
    private static final long COMMENT_ID = 17000000000000001L;

    @Autowired private FootballTeamMapper teamMapper;
    @Autowired private FootballPlayerMapper playerMapper;
    @Autowired private UserProfileMapper profileMapper;
    @Autowired private ContentMapper contentMapper;
    @Autowired private CommentMapper commentMapper;
    @Autowired private FeedService feedService;
    @Autowired private SearchService searchService;

    @BeforeEach
    void requireConfiguredDevDatabase() {
        Assumptions.assumeTrue(System.getenv("MYSQL_PASSWORD") != null
                        && !System.getenv("MYSQL_PASSWORD").isBlank(),
                "MYSQL_PASSWORD is required for demo data integration assertions");
    }

    @Test
    void fixedDemoRowsAndFeedKeepChineseText() {
        FootballTeam team = teamMapper.selectById(TEAM_ID);
        Assumptions.assumeTrue(team != null, "T14 demo dataset is not imported");
        FootballPlayer player = playerMapper.selectById(PLAYER_ID);
        UserProfile profile = profileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<UserProfile>().eq("user_id", USER_ID));
        Content content = contentMapper.selectById(CONTENT_ID);
        Comment comment = commentMapper.selectById(COMMENT_ID);

        assertChinese(team.getTeamName());
        assertChinese(player.getPlayerName());
        assertChinese(profile.getNickname());
        assertChinese(content.getTitle());
        assertChinese(content.getBody());
        assertChinese(comment.getContentText());

        FeedCardVO contentCard = feedService.feed("recommend", null, null, 1, 20, null).getRecords().stream()
                .filter(card -> card.getContentId() != null)
                .findFirst()
                .orElseThrow();
        assertChinese(contentCard.getTitle());
    }

    @Test
    void chineseEntitySearchFindsDemoTeamAndPlayer() {
        Assumptions.assumeTrue(teamMapper.selectById(TEAM_ID) != null, "T14 demo dataset is not imported");
        PageResult<SearchEntityVO> teams = searchService.entities("\u963f\u68ee\u7eb3", "TEAM", 1L, 20L);
        PageResult<SearchEntityVO> players = searchService.entities("\u6797\u8fdc\u822a", "PLAYER", 1L, 20L);

        assertThat(teams.getRecords()).extracting(SearchEntityVO::getEntityId).contains(TEAM_ID);
        assertThat(players.getRecords()).extracting(SearchEntityVO::getEntityId).contains(PLAYER_ID);
        assertChinese(teams.getRecords().get(0).getName());
        assertChinese(players.getRecords().get(0).getName());
    }

    private void assertChinese(String value) {
        assertThat(value).isNotBlank().containsPattern("[\\u4E00-\\u9FFF]").doesNotContain("??");
        assertThat(value).doesNotMatch("^\\?+$");
    }
}
