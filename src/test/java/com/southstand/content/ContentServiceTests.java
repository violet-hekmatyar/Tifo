package com.southstand.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.content.dto.CreatePostRequest;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.mapper.ContentMediaMapper;
import com.southstand.content.mapper.ContentRelationMapper;
import com.southstand.content.service.ContentService;
import com.southstand.content.vo.CreatePostResponse;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ContentServiceTests {

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void contentNotFoundReturns40401() {
        ContentService service = service(mock(ContentMapper.class), mock(UserProfileMapper.class));

        assertThatThrownBy(() -> service.detail(99999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void createPostIncrementsProfilePostCount() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        ContentMapper contentMapper = mock(ContentMapper.class);
        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        when(contentMapper.insert(any(Content.class))).thenAnswer(invocation -> {
            Content content = invocation.getArgument(0);
            content.setId(22001L);
            return 1;
        });

        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("demo post");
        request.setBody("body");
        request.setMediaUrls(List.of("/uploads/content/demo.jpg"));

        CreatePostResponse response = service(contentMapper, profileMapper).createPost(request);

        assertThat(response.getContentId()).isEqualTo(22001L);
        verify(profileMapper).update(any(), any());
    }

    private ContentService service(ContentMapper contentMapper, UserProfileMapper profileMapper) {
        return new ContentService(
                contentMapper,
                mock(ContentMediaMapper.class),
                mock(ContentRelationMapper.class),
                mock(LikeRecordMapper.class),
                mock(FavoriteRecordMapper.class),
                mock(SysUserMapper.class),
                profileMapper,
                mock(FootballTeamMapper.class),
                mock(FootballPlayerMapper.class)
        );
    }
}
