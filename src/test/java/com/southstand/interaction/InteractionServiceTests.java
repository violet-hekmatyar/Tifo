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
import com.southstand.interaction.dto.ToggleFavoriteRequest;
import com.southstand.interaction.dto.ToggleLikeRequest;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.entity.FavoriteRecord;
import com.southstand.interaction.entity.LikeRecord;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.interaction.service.InteractionService;
import com.southstand.interaction.vo.ToggleFavoriteResponse;
import com.southstand.interaction.vo.ToggleLikeResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class InteractionServiceTests {

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void contentLikeToggleActivatesAndCancels() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        LikeRecordMapper likeMapper = mock(LikeRecordMapper.class);
        ContentService contentService = mock(ContentService.class);
        when(contentService.requireVisibleContent(20001L)).thenReturn(new Content());
        ToggleLikeRequest request = new ToggleLikeRequest();
        request.setTargetType("CONTENT");
        request.setTargetId(20001L);

        when(likeMapper.selectOne(any())).thenReturn(null);
        ToggleLikeResponse liked = service(likeMapper, mock(FavoriteRecordMapper.class), mock(CommentMapper.class), contentService).toggleLike(request);
        assertThat(liked.getLiked()).isTrue();

        LikeRecord active = new LikeRecord();
        active.setId(90001L);
        active.setStatus("ACTIVE");
        when(likeMapper.selectOne(any())).thenReturn(active);
        ToggleLikeResponse cancelled = service(likeMapper, mock(FavoriteRecordMapper.class), mock(CommentMapper.class), contentService).toggleLike(request);
        assertThat(cancelled.getLiked()).isFalse();
    }

    @Test
    void commentLikeToggleUsesCommentCount() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        LikeRecordMapper likeMapper = mock(LikeRecordMapper.class);
        CommentMapper commentMapper = mock(CommentMapper.class);
        Comment comment = new Comment();
        comment.setId(60001L);
        comment.setStatus("ACTIVE");
        comment.setIsDeleted(0);
        when(commentMapper.selectById(60001L)).thenReturn(comment);
        ToggleLikeRequest request = new ToggleLikeRequest();
        request.setTargetType("COMMENT");
        request.setTargetId(60001L);

        ToggleLikeResponse response = service(likeMapper, mock(FavoriteRecordMapper.class), commentMapper, mock(ContentService.class)).toggleLike(request);

        assertThat(response.getLiked()).isTrue();
        verify(commentMapper).update(any(), any());
    }

    @Test
    void favoriteToggleActivatesAndCancels() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        FavoriteRecordMapper favoriteMapper = mock(FavoriteRecordMapper.class);
        ContentService contentService = mock(ContentService.class);
        when(contentService.requireVisibleContent(20001L)).thenReturn(new Content());
        ToggleFavoriteRequest request = new ToggleFavoriteRequest();
        request.setTargetType("CONTENT");
        request.setTargetId(20001L);

        ToggleFavoriteResponse favorited = service(mock(LikeRecordMapper.class), favoriteMapper, mock(CommentMapper.class), contentService).toggleFavorite(request);
        assertThat(favorited.getFavorited()).isTrue();

        FavoriteRecord active = new FavoriteRecord();
        active.setId(91001L);
        active.setStatus("ACTIVE");
        when(favoriteMapper.selectOne(any())).thenReturn(active);
        ToggleFavoriteResponse cancelled = service(mock(LikeRecordMapper.class), favoriteMapper, mock(CommentMapper.class), contentService).toggleFavorite(request);
        assertThat(cancelled.getFavorited()).isFalse();
    }

    private InteractionService service(LikeRecordMapper likeMapper,
            FavoriteRecordMapper favoriteMapper,
            CommentMapper commentMapper,
            ContentService contentService) {
        return new InteractionService(
                likeMapper,
                favoriteMapper,
                mock(ContentMapper.class),
                commentMapper,
                contentService
        );
    }
}
