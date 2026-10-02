package com.southstand.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.entity.Content;
import com.southstand.follow.service.FollowService;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.player.mapper.TeamPlayerMapper;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import com.southstand.user.service.UserProfileService;
import com.southstand.user.vo.UserProfileVO;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UserProfileServiceTests {

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void profileDoesNotReturnPasswordHash() throws Exception {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        SysUserMapper userMapper = mock(SysUserMapper.class);
        UserProfileMapper profileMapper = mock(UserProfileMapper.class);
        FollowService followService = mock(FollowService.class);

        SysUser user = new SysUser();
        user.setId(10002L);
        user.setUsername("test_user");
        user.setPasswordHash("hash_should_not_appear");
        user.setRoleType("USER");
        user.setStatus("ACTIVE");
        user.setIsDeleted(0);
        user.setOnboardingCompleted(1);
        UserProfile profile = new UserProfile();
        profile.setUserId(10002L);
        profile.setNickname("南看台老球迷");
        profile.setFollowerCount(0);

        when(userMapper.selectById(10002L)).thenReturn(user);
        when(profileMapper.selectOne(any())).thenReturn(profile);
        when(followService.syncUserProfileCounts(10002L)).thenReturn(new FollowService.FollowCounts(0, 0, 0));
        when(followService.findActiveRecords(any(), any())).thenReturn(List.of());

        UserProfileVO vo = new UserProfileService(
                userMapper,
                profileMapper,
                mock(FootballTeamMapper.class),
                mock(FootballPlayerMapper.class),
                mock(TeamPlayerMapper.class),
                followService,
                mock(ContentMapper.class),
                mock(FavoriteRecordMapper.class),
                mock(com.southstand.interaction.mapper.LikeRecordMapper.class),
                mock(CommentMapper.class)
        ).me();

        String json = new ObjectMapper().writeValueAsString(vo);
        assertThat(json).doesNotContain("passwordHash");
        assertThat(json).doesNotContain("hash_should_not_appear");
    }

    @Test
    void myContentsBackfillsTotalWhenPageRecordsExistWithoutPaginationCount() {
        CurrentUserHolder.set(new LoginUserContext(10002L, "test_user", "USER"));
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ContentMapper contentMapper = mock(ContentMapper.class);
        SysUser user = new SysUser();
        user.setId(10002L);
        user.setStatus("ACTIVE");
        user.setIsDeleted(0);
        when(userMapper.selectById(10002L)).thenReturn(user);

        Content content = new Content();
        content.setId(20001L);
        content.setAuthorId(10002L);
        content.setContentType("POST");
        content.setTitle("测试内容");
        content.setStatus("PUBLISHED");
        content.setIsDeleted(0);
        Page<Content> page = new Page<>(1, 10);
        page.setRecords(List.of(content));
        page.setTotal(0);
        when(contentMapper.selectPage(any(), any())).thenReturn(page);
        when(contentMapper.selectCount(any())).thenReturn(3L);

        UserProfileService service = new UserProfileService(
                userMapper,
                mock(UserProfileMapper.class),
                mock(FootballTeamMapper.class),
                mock(FootballPlayerMapper.class),
                mock(TeamPlayerMapper.class),
                mock(FollowService.class),
                contentMapper,
                mock(FavoriteRecordMapper.class),
                mock(com.southstand.interaction.mapper.LikeRecordMapper.class),
                mock(CommentMapper.class)
        );

        var result = service.myContents(1L, 10L, null);

        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getTotal()).isEqualTo(3L);
        assertThat(result.getPages()).isEqualTo(1L);
    }
}
