package com.southstand.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.notification.controller.NotificationController;
import com.southstand.notification.service.NotificationService;
import com.southstand.notification.vo.ReadAllVO;
import com.southstand.notification.vo.UnreadCountVO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationControllerTests {
    @Test void delegatesListCountAndReadOperations(){
        NotificationService service=mock(NotificationService.class);NotificationController c=new NotificationController(service);
        when(service.list(2,10,"CONTENT_LIKED","UNREAD")).thenReturn(PageResult.of(List.of(),0,2,10));
        when(service.unreadCount()).thenReturn(new UnreadCountVO(2,Map.of()));when(service.read(9L)).thenReturn(true);when(service.readAll()).thenReturn(new ReadAllVO(2));
        c.list(2L,10L,"CONTENT_LIKED","UNREAD");c.unreadCount();c.read(9L);c.readAll();
        verify(service).list(2,10,"CONTENT_LIKED","UNREAD");verify(service).read(9L);verify(service).readAll();
    }

    @Test void serviceRejectsUnauthorizedWithoutLeakingRows(){
        NotificationService service=new NotificationService(mock(com.southstand.notification.mapper.NotificationMapper.class),mock(com.southstand.notification.service.NotificationWriter.class),mock(com.southstand.content.mapper.ContentMapper.class),mock(com.southstand.interaction.mapper.CommentMapper.class),mock(com.southstand.user.mapper.SysUserMapper.class),mock(com.southstand.user.mapper.UserProfileMapper.class));
        assertThatThrownBy(()->service.read(999L)).isInstanceOfSatisfying(BusinessException.class,e->org.assertj.core.api.Assertions.assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
