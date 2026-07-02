package com.southstand.auth.security;

public class LoginUserContext {

    private final Long userId;
    private final String username;
    private final String roleType;

    public LoginUserContext(Long userId, String username, String roleType) {
        this.userId = userId;
        this.username = username;
        this.roleType = roleType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRoleType() {
        return roleType;
    }
}
