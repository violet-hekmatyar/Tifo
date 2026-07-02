package com.southstand.common.enums;

public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(40001, "参数错误"),
    UNAUTHORIZED(40101, "未登录"),
    TOKEN_EXPIRED(40102, "登录已过期"),
    LOGIN_FAILED_TOO_MANY_TIMES(40103, "登录失败次数过多"),
    FORBIDDEN(40301, "无权限"),
    NOT_FOUND(40401, "资源不存在"),
    CONFLICT(40901, "数据冲突"),
    TEAM_FOLLOW_LIMIT(40902, "关注球队数量已达上限"),
    SYSTEM_ERROR(50001, "系统异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
