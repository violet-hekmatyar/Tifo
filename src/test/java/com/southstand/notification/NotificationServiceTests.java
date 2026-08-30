package com.southstand.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.notification.entity.Notification;
import com.southstand.notification.mapper.NotificationMapper;
import com.southstand.notification.service.NotificationService;
import com.southstand.notification.service.NotificationWriter;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationServiceTests {
    @AfterEach void clear(){CurrentUserHolder.clear();}

    @Test void createsAllInteractionTypesAndSkipsSelf(){
        Fixture f=new Fixture();
        Content content=content(10L,2L,"title",0);when(f.contents.selectById(10L)).thenReturn(content);
        Comment comment=comment(20L,2L,10L,0);when(f.comments.selectById(20L)).thenReturn(comment);
        f.service.notifyContentLiked(1L,10L);
        f.service.notifyContentCommented(1L,10L,20L);
        f.service.notifyCommentReplied(1L,2L,21L,10L);
        f.service.notifyCommentLiked(1L,20L);
        f.service.notifyUserFollowed(1L,2L);com.southstand.user.entity.SysUser recipient=new com.southstand.user.entity.SysUser();recipient.setId(2L);recipient.setStatus("ACTIVE");recipient.setIsDeleted(0);when(f.users.selectById(2L)).thenReturn(recipient);
        f.service.createSystemNotification(2L,"系统","维护完成","maintenance-1");
        ArgumentCaptor<Notification> captor=ArgumentCaptor.forClass(Notification.class);
        verify(f.writer,org.mockito.Mockito.times(6)).persist(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getNotificationType)
                .containsExactly("CONTENT_LIKED","CONTENT_COMMENTED","COMMENT_REPLIED","COMMENT_LIKED","USER_FOLLOWED","SYSTEM");
        f.service.notifyContentLiked(2L,10L);
        verify(f.writer,org.mockito.Mockito.times(6)).persist(any());
    }

    @Test void listUsesDatabasePageAndHandlesDeletedTarget(){
        Fixture f=new Fixture();CurrentUserHolder.set(new LoginUserContext(2L,"b","USER"));
        Notification n=notification(100L,"CONTENT_LIKED","CONTENT",10L);
        when(f.notifications.countNotificationPage(2L,"CONTENT_LIKED",0)).thenReturn(1L);
        when(f.notifications.selectNotificationPage(2L,"CONTENT_LIKED",0,0,20)).thenReturn(List.of(n));
        when(f.profiles.selectList(any())).thenReturn(List.of(profile(1L,"甲")));
        when(f.contents.selectBatchIds(any())).thenReturn(List.of(content(10L,3L,"已删除",1)));
        var page=f.service.list(1,20,"CONTENT_LIKED","UNREAD");
        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords()).singleElement().satisfies(v->{assertThat(v.actor().nickname()).isEqualTo("甲");assertThat(v.targetAvailable()).isFalse();});
    }

    private static class Fixture{
        NotificationMapper notifications=mock(NotificationMapper.class);NotificationWriter writer=mock(NotificationWriter.class);
        ContentMapper contents=mock(ContentMapper.class);CommentMapper comments=mock(CommentMapper.class);
        SysUserMapper users=mock(SysUserMapper.class);UserProfileMapper profiles=mock(UserProfileMapper.class);
        NotificationService service=new NotificationService(notifications,writer,contents,comments,users,profiles);
        Fixture(){when(profiles.selectOne(any())).thenReturn(profile(1L,"甲"));}
    }
    static Content content(Long id,Long author,String title,int deleted){Content c=new Content();c.setId(id);c.setAuthorId(author);c.setTitle(title);c.setStatus("PUBLISHED");c.setIsDeleted(deleted);return c;}
    static Comment comment(Long id,Long user,Long target,int deleted){Comment c=new Comment();c.setId(id);c.setUserId(user);c.setTargetId(target);c.setContentText("评论预览");c.setStatus("ACTIVE");c.setIsDeleted(deleted);return c;}
    static UserProfile profile(Long id,String name){UserProfile p=new UserProfile();p.setUserId(id);p.setNickname(name);return p;}
    static Notification notification(Long id,String type,String targetType,Long targetId){Notification n=new Notification();n.setId(id);n.setRecipientUserId(2L);n.setActorUserId(1L);n.setNotificationType(type);n.setTargetType(targetType);n.setTargetId(targetId);n.setReadFlag(0);n.setStatus("ACTIVE");n.setIsDeleted(0);n.setCreateTime(LocalDateTime.now());return n;}
}
