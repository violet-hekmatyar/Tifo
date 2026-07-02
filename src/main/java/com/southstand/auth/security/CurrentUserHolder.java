package com.southstand.auth.security;

import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;

public final class CurrentUserHolder {

    private static final ThreadLocal<LoginUserContext> HOLDER = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(LoginUserContext context) {
        HOLDER.set(context);
    }

    public static LoginUserContext get() {
        LoginUserContext context = HOLDER.get();
        if (context == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return context;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
