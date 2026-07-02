package com.southstand.auth.vo;

public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfoVO user;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserInfoVO getUser() {
        return user;
    }

    public void setUser(UserInfoVO user) {
        this.user = user;
    }
}
