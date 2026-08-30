package com.southstand.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.service.ContentService;
import com.southstand.interaction.dto.CreateCommentRequest;
import com.southstand.interaction.dto.ToggleLikeRequest;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.interaction.service.CommentService;
import com.southstand.interaction.service.InteractionService;
import com.southstand.follow.dto.FollowToggleRequest;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.follow.service.FollowService;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.notification.mapper.NotificationMapper;
import com.southstand.notification.service.NotificationService;
import com.southstand.notification.service.NotificationWriter;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import com.southstand.user.entity.SysUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NotificationTriggerIntegrationTests {
    @AfterEach void clear(){CurrentUserHolder.clear();}
    @Test void likeCommentReplyAndCommentLikeTriggerNotifications(){
        CurrentUserHolder.set(new LoginUserContext(1L,"a","USER"));NotificationService notifications=mock(NotificationService.class);
        ContentMapper cm=mock(ContentMapper.class);CommentMapper comments=mock(CommentMapper.class);ContentService cs=mock(ContentService.class);
        Content content=new Content();content.setId(10L);content.setAuthorId(2L);when(cs.requireVisibleContent(10L)).thenReturn(content);
        ToggleLikeRequest like=new ToggleLikeRequest();like.setTargetType("CONTENT");like.setTargetId(10L);
        assertThat(new InteractionService(mock(LikeRecordMapper.class),mock(FavoriteRecordMapper.class),cm,comments,cs,null,notifications).toggleLike(like).getLiked()).isTrue();
        verify(notifications).notifyContentLiked(1L,10L);

        when(comments.insert(any(Comment.class))).thenAnswer(i->{((Comment)i.getArgument(0)).setId(20L);return 1;});
        CreateCommentRequest root=new CreateCommentRequest();root.setTargetType("CONTENT");root.setTargetId(10L);root.setParentId(0L);root.setContentText("root");
        new CommentService(comments,cm,mock(LikeRecordMapper.class),cs,null,notifications).create(root);
        verify(notifications).notifyContentCommented(1L,10L,20L);
        Comment parent=NotificationServiceTests.comment(20L,2L,10L,0);parent.setParentId(0L);parent.setTargetType("CONTENT");when(comments.selectById(20L)).thenReturn(parent);
        CreateCommentRequest reply=new CreateCommentRequest();reply.setTargetType("CONTENT");reply.setTargetId(10L);reply.setParentId(20L);reply.setContentText("reply");
        new CommentService(comments,cm,mock(LikeRecordMapper.class),cs,null,notifications).create(reply);
        verify(notifications).notifyCommentReplied(1L,2L,20L,10L);

        when(comments.selectById(20L)).thenReturn(parent);
        new CommentService(comments,cm,mock(LikeRecordMapper.class),cs,null,notifications).toggleLike(20L);
        verify(notifications).notifyCommentLiked(1L,20L);

        FollowRecordMapper follows=mock(FollowRecordMapper.class);SysUser target=new SysUser();target.setId(2L);target.setStatus("ACTIVE");target.setIsDeleted(0);SysUserMapper users=mock(SysUserMapper.class);when(users.selectById(2L)).thenReturn(target);when(follows.selectCount(any())).thenReturn(0L);
        FollowToggleRequest follow=new FollowToggleRequest();follow.setFollowType("USER");follow.setTargetId(2L);
        new FollowService(follows,mock(FootballTeamMapper.class),mock(FootballPlayerMapper.class),users,mock(UserProfileMapper.class),notifications).toggle(follow);
        verify(notifications).notifyUserFollowed(1L,2L);
    }

    @Test void notificationWriteFailureDoesNotBreakMainLike(){
        CurrentUserHolder.set(new LoginUserContext(1L,"a","USER"));ContentMapper cm=mock(ContentMapper.class);ContentService cs=mock(ContentService.class);
        Content c=NotificationServiceTests.content(10L,2L,"x",0);when(cs.requireVisibleContent(10L)).thenReturn(c);when(cm.selectById(10L)).thenReturn(c);
        NotificationWriter writer=mock(NotificationWriter.class);doThrow(new IllegalStateException("offline")).when(writer).persist(any());
        NotificationService notifications=new NotificationService(mock(NotificationMapper.class),writer,cm,mock(CommentMapper.class),mock(SysUserMapper.class),mock(UserProfileMapper.class));
        ToggleLikeRequest req=new ToggleLikeRequest();req.setTargetType("CONTENT");req.setTargetId(10L);
        assertThat(new InteractionService(mock(LikeRecordMapper.class),mock(FavoriteRecordMapper.class),cm,mock(CommentMapper.class),cs,null,notifications).toggleLike(req).getLiked()).isTrue();
    }
}
