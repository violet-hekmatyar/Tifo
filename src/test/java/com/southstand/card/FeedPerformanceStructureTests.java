package com.southstand.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.card.service.FeedService;
import com.southstand.card.vo.FeedPageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.mapper.ContentRelationMapper;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.football.event.mapper.MatchEventMapper;
import com.southstand.football.league.mapper.FootballLeagueMapper;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.report.mapper.MatchReportMapper;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class FeedPerformanceStructureTests {

    @Test
    void contentPageUsesBoundedBatchQueriesInsteadOfPerCardQueries() {
        ContentMapper contentMapper = mock(ContentMapper.class);
        ContentRelationMapper relationMapper = mock(ContentRelationMapper.class);
        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        CommentMapper commentMapper = mock(CommentMapper.class);
        LikeRecordMapper likeMapper = mock(LikeRecordMapper.class);
        FavoriteRecordMapper favoriteMapper = mock(FavoriteRecordMapper.class);
        List<Content> contents = LongStream.rangeClosed(1, 40).mapToObj(this::content).toList();
        when(contentMapper.selectList(any(Wrapper.class))).thenReturn(contents);
        when(relationMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(profileMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(commentMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        FeedService service = new FeedService(
                contentMapper, relationMapper, mock(MatchInfoMapper.class), mock(MatchEventMapper.class),
                mock(MatchReportMapper.class), mock(FootballLeagueMapper.class), mock(FootballTeamMapper.class),
                mock(FootballPlayerMapper.class), mock(FollowRecordMapper.class), profileMapper,
                likeMapper, favoriteMapper, commentMapper);

        FeedPageResult page = service.feed("news", null, null, 1, 20, null);

        assertThat(page.getRecords()).hasSize(20);
        assertThat(page.getTotal()).isEqualTo(40);
        verify(contentMapper, times(1)).selectList(any(Wrapper.class));
        verify(relationMapper, times(1)).selectList(any(Wrapper.class));
        verify(profileMapper, times(1)).selectList(any(Wrapper.class));
        verify(commentMapper, times(1)).selectList(any(Wrapper.class));
        verify(profileMapper, never()).selectOne(any(Wrapper.class));
        verify(likeMapper, never()).selectCount(any(Wrapper.class));
        verify(favoriteMapper, never()).selectCount(any(Wrapper.class));
    }

    private Content content(long id) {
        Content content = new Content();
        content.setId(id);
        content.setAuthorId(1000L + id % 5);
        content.setContentType("NEWS");
        content.setTitle("title " + id);
        content.setLikeCount(1);
        content.setCommentCount(1);
        content.setFavoriteCount(1);
        content.setHotScore(BigDecimal.ONE);
        content.setPublishTime(LocalDateTime.now());
        content.setStatus("PUBLISHED");
        content.setIsDeleted(0);
        return content;
    }
}
