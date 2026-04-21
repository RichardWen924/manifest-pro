package com.manifestreader.admin.model.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminLoginRequest {

    @NotBlank(message = "username cannot be blank")
    private String username;

    @NotBlank(message = "password cannot be blank")
    private String password;

    private String role;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
