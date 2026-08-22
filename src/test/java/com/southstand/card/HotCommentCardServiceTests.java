package com.southstand.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.southstand.card.model.HomeFeedUserContext;
import com.southstand.card.service.HotCommentCardService;
import com.southstand.card.vo.HotCommentCardPayload;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.service.CommentService;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.UserProfileMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class HotCommentCardServiceTests {
    @Test
    void usesT12HotFormulaAndOnlyVisibleParentsAndValidAuthors() {
        CommentMapper comments = mock(CommentMapper.class); ContentMapper contents = mock(ContentMapper.class); UserProfileMapper profiles = mock(UserProfileMapper.class);
        Comment valid = comment(1L, 10L, 7L, 5, 3); Comment hiddenParent = comment(2L, 11L, 7L, 99, 9);
        when(comments.selectList(any(Wrapper.class))).thenReturn(List.of(hiddenParent, valid));
        when(contents.selectList(any(Wrapper.class))).thenReturn(List.of(content(10L)));
        when(profiles.selectList(any(Wrapper.class))).thenReturn(List.of(profile(7L)));
        var cards = new HotCommentCardService(comments, contents, profiles).candidates(HomeFeedUserContext.anonymous());
        assertThat(cards).hasSize(1);
        HotCommentCardPayload payload = (HotCommentCardPayload) cards.get(0).getPayload();
        assertThat(payload.hotScore()).isEqualByComparingTo(CommentService.calculateHotScore(5, 3, valid.getCreateTime(), valid.getCreateTime()));
        assertThat(cards.get(0).getReason()).isEqualTo("热门评论");
    }
    private Comment comment(long id,long target,long user,int like,int reply){Comment c=new Comment();c.setId(id);c.setTargetId(target);c.setUserId(user);c.setContentText("评论");c.setLikeCount(like);c.setReplyCount(reply);c.setCreateTime(LocalDateTime.now());return c;}
    private Content content(long id){Content c=new Content();c.setId(id);c.setTitle("内容");c.setContentType("NEWS");return c;}
    private UserProfile profile(long id){UserProfile p=new UserProfile();p.setUserId(id);p.setNickname("作者");return p;}
}
