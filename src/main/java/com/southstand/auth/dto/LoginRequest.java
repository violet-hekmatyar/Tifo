package com.southstand.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "username is required")
    @Size(max = 64, message = "username length must be <= 64")
    private String username;

    @NotBlank(message = "password is required")
    @Size(max = 64, message = "password length must be <= 64")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
