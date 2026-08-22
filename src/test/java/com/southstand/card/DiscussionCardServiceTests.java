package com.southstand.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.card.model.HomeFeedUserContext;
import com.southstand.card.service.DiscussionCardService;
import com.southstand.content.entity.Content;
import com.southstand.content.entity.ContentRelation;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.mapper.ContentRelationMapper;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiscussionCardServiceTests {
    @Test
    void commentWeightAndMainTeamBonusProduceStableDiscussionOrder() {
        ContentMapper contents = mock(ContentMapper.class); ContentRelationMapper relations = mock(ContentRelationMapper.class);
        CommentMapper comments = mock(CommentMapper.class); UserProfileMapper profiles = mock(UserProfileMapper.class);
        Content main = content(1L, 2, 1); Content hot = content(2L, 10, 5);
        when(contents.selectList(any(Wrapper.class))).thenReturn(List.of(main, hot));
        when(relations.selectList(any(Wrapper.class))).thenReturn(List.of(relation(1L, 9L)));
        when(comments.selectList(any(Wrapper.class))).thenReturn(List.of(comment(10L, 1L, 7L)));
        when(profiles.selectList(any(Wrapper.class))).thenReturn(List.of(profile(7L)));
        var cards = new DiscussionCardService(contents, relations, comments, profiles)
                .candidates(new HomeFeedUserContext(7L, 9L, Set.of(), Set.of(), Set.of()));
        assertThat(cards).hasSize(2);
        assertThat(cards.get(0).getContentId()).isEqualTo(1L);
        assertThat(cards.get(0).getReasonCode()).isEqualTo("MAIN_TEAM");
        assertThat(cards.get(0).getCardKey()).isEqualTo("DISCUSSION:1");
        assertThat(cards.get(0).getHotComment()).isNotNull();
    }

    @Test
    void noVisiblePostMeansNoCard() {
        ContentMapper contents = mock(ContentMapper.class);
        when(contents.selectList(any(Wrapper.class))).thenReturn(List.of());
        assertThat(new DiscussionCardService(contents, mock(ContentRelationMapper.class), mock(CommentMapper.class),
                mock(UserProfileMapper.class)).candidates(HomeFeedUserContext.anonymous())).isEmpty();
    }

    private Content content(long id, int comments, int likes) { Content c = new Content(); c.setId(id); c.setContentType("POST"); c.setTitle("讨论" + id); c.setAuthorId(7L); c.setCommentCount(comments); c.setLikeCount(likes); c.setFavoriteCount(1); c.setHotScore(BigDecimal.ONE); c.setPublishTime(LocalDateTime.now().minusHours(id)); return c; }
    private ContentRelation relation(long content, long team) { ContentRelation r = new ContentRelation(); r.setId(content); r.setContentId(content); r.setRelationType("TEAM"); r.setRelationId(team); return r; }
    private Comment comment(long id, long content, long user) { Comment c = new Comment(); c.setId(id); c.setTargetId(content); c.setUserId(user); c.setContentText("高质量热评"); c.setLikeCount(5); c.setReplyCount(3); c.setCreateTime(LocalDateTime.now()); return c; }
    private UserProfile profile(long id) { UserProfile p = new UserProfile(); p.setUserId(id); p.setNickname("用户"); return p; }
}
