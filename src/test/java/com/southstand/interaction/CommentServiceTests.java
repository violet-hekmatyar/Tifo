package com.southstand.interaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.service.ContentService;
import com.southstand.interaction.dto.CreateCommentRequest;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.interaction.service.CommentService;
import com.southstand.interaction.vo.CreateCommentResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CommentServiceTests {

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void hotScoreUsesTimeCoefficient() {
        LocalDateTime now = LocalDateTime.now();

        assertThat(CommentService.calculateHotScore(2, 2, now.minusHours(1), now)).isEqualByComparingTo("9.00");
        assertThat(CommentService.calculateHotScore(2, 2, now.minusHours(6), now)).isEqualByComparingTo("6.00");
        assertThat(CommentService.calculateHotScore(2, 2, now.minusHours(18), now)).isEqualByComparingTo("4.20");
        assertThat(CommentService.calculateHotScore(2, 2, now.minusHours(30), now)).isEqualByComparingTo("1.80");
    }

    @Test
    void createRootCommentIncrementsContentCommentCount() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        CommentMapper commentMapper = mock(CommentMapper.class);
        ContentMapper contentMapper = mock(ContentMapper.class);
        ContentService contentService = mock(ContentService.class);
        when(contentService.requireVisibleContent(20001L)).thenReturn(new Content());
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(60010L);
            return 1;
        });

        CreateCommentRequest request = new CreateCommentRequest();
        request.setTargetType("CONTENT");
        request.setTargetId(20001L);
        request.setParentId(0L);
        request.setContentText("good match");

        CreateCommentResponse response = new CommentService(commentMapper, contentMapper, mock(LikeRecordMapper.class), contentService).create(request);

        assertThat(response.getCommentId()).isEqualTo(60010L);
        verify(contentMapper).update(any(), any());
    }

    @Test
    void replyIncrementsParentReplyCount() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        CommentMapper commentMapper = mock(CommentMapper.class);
        ContentService contentService = mock(ContentService.class);
        when(contentService.requireVisibleContent(20001L)).thenReturn(new Content());
        Comment parent = new Comment();
        parent.setId(60001L);
        parent.setTargetType("CONTENT");
        parent.setTargetId(20001L);
        parent.setParentId(0L);
        parent.setStatus("ACTIVE");
        parent.setIsDeleted(0);
        when(commentMapper.selectById(60001L)).thenReturn(parent);
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(60011L);
            return 1;
        });

        CreateCommentRequest request = new CreateCommentRequest();
        request.setTargetType("CONTENT");
        request.setTargetId(20001L);
        request.setParentId(60001L);
        request.setContentText("reply");

        CreateCommentResponse response = new CommentService(commentMapper, mock(ContentMapper.class), mock(LikeRecordMapper.class), contentService).create(request);

        assertThat(response.getCommentId()).isEqualTo(60011L);
        verify(commentMapper).update(any(), any());
    }
}
