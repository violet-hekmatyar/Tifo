package com.southstand.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.notification.entity.Notification;
import com.southstand.notification.mapper.NotificationMapper;
import com.southstand.notification.service.NotificationService;
import com.southstand.notification.service.NotificationWriter;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NotificationPerformanceStructureTests {
    @AfterEach void clear(){CurrentUserHolder.clear();}
    @Test void fortyNotificationsUseFixedBatchQueries(){
        CurrentUserHolder.set(new LoginUserContext(2L,"b","USER"));NotificationMapper nm=mock(NotificationMapper.class);ContentMapper cm=mock(ContentMapper.class);CommentMapper comments=mock(CommentMapper.class);UserProfileMapper profiles=mock(UserProfileMapper.class);
        List<Notification> rows=IntStream.range(0,40).mapToObj(i->NotificationServiceTests.notification((long)i,"CONTENT_LIKED",i%2==0?"CONTENT":"COMMENT",100L+i)).toList();
        when(nm.countNotificationPage(2L,null,null)).thenReturn(40L);when(nm.selectNotificationPage(2L,null,null,0,40)).thenReturn(rows);when(profiles.selectList(any())).thenReturn(List.of());when(cm.selectBatchIds(any())).thenReturn(List.of());when(comments.selectBatchIds(any())).thenReturn(List.of());
        new NotificationService(nm,mock(NotificationWriter.class),cm,comments,mock(SysUserMapper.class),profiles).list(1,40,null,null);
        verify(profiles,times(1)).selectList(any());verify(cm,times(1)).selectBatchIds(any());verify(comments,times(1)).selectBatchIds(any());
    }
}
