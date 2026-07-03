package com.southstand.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.southstand.admin.dto.AdminUserStatusRequest;
import com.southstand.admin.mapper.AdminOperationLogMapper;
import com.southstand.admin.service.AdminService;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.user.entity.SysUser;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AdminServiceTests {

    @AfterEach
    void tearDown() {
        CurrentUserHolder.clear();
    }

    @Test
    void adminCannotDisableSelf() {
        CurrentUserHolder.set(new LoginUserContext(10001L, "admin", "ADMIN"));
        AdminUserStatusRequest request = new AdminUserStatusRequest();
        request.setStatus("DISABLED");

        assertThatThrownBy(() -> service(mock(SysUserMapper.class)).updateUserStatus(10001L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PARAM_ERROR));
    }

    @Test
    void updateMissingUserStatusReturnsNotFound() {
        CurrentUserHolder.set(new LoginUserContext(10001L, "admin", "ADMIN"));
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);
        when(sysUserMapper.selectById(10002L)).thenReturn(null);
        AdminUserStatusRequest request = new AdminUserStatusRequest();
        request.setStatus("DISABLED");

        assertThatThrownBy(() -> service(sysUserMapper).updateUserStatus(10002L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void updateUserStatusAllowsActiveAndDisabledOnly() {
        CurrentUserHolder.set(new LoginUserContext(10001L, "admin", "ADMIN"));
        AdminUserStatusRequest request = new AdminUserStatusRequest();
        request.setStatus("DELETED");

        assertThatThrownBy(() -> service(mock(SysUserMapper.class)).updateUserStatus(10002L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PARAM_ERROR));
    }

    private AdminService service(SysUserMapper sysUserMapper) {
        return new AdminService(
                sysUserMapper,
                mock(UserProfileMapper.class),
                mock(ContentMapper.class),
                mock(CommentMapper.class),
                mock(FavoriteRecordMapper.class),
                mock(MatchInfoMapper.class),
                mock(AdminOperationLogMapper.class)
        );
    }
}
