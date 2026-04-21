package com.manifest.auth.service;

import com.manifest.auth.dto.LoginRequest;
import com.manifest.auth.dto.RefreshTokenRequest;
import com.manifest.auth.dto.TokenIntrospectionRequest;
import com.manifest.auth.vo.CurrentUserResponse;
import com.manifest.auth.vo.LoginResponse;
import com.manifest.auth.vo.TokenIntrospectionResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(String authorization);

    LoginResponse refresh(RefreshTokenRequest request);

    TokenIntrospectionResponse introspect(TokenIntrospectionRequest request);

    CurrentUserResponse me(String authorization);

    String publicKey();
}
