package com.manifest.auth.service.impl;

import com.manifest.auth.dto.LoginRequest;
import com.manifest.auth.dto.RefreshTokenRequest;
import com.manifest.auth.dto.TokenIntrospectionRequest;
import com.manifest.auth.security.KeyProvider;
import com.manifest.auth.service.AuthService;
import com.manifest.auth.token.IssuedToken;
import com.manifest.auth.token.TokenPrincipal;
import com.manifest.auth.token.TokenService;
import com.manifest.auth.vo.CurrentUserResponse;
import com.manifest.auth.vo.LoginResponse;
import com.manifest.auth.vo.TokenIntrospectionResponse;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import java.util.Collections;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final TokenService tokenService;
    private final KeyProvider keyProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(TokenService tokenService, KeyProvider keyProvider, PasswordEncoder passwordEncoder) {
        this.tokenService = tokenService;
        this.keyProvider = keyProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        // TODO 查询用户、使用 passwordEncoder.matches 校验 BCrypt 密码、记录登录日志。
        passwordEncoder.encode(request.password());
        IssuedToken token = tokenService.issue(new TokenPrincipal(
                0L,
                0L,
                request.username(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null,
                null
        ));
        return new LoginResponse(token.accessToken(), token.refreshToken(), token.tokenType(), token.expiresAt());
    }

    @Override
    public void logout(String authorization) {
        // TODO 解析 accessToken jti，写入 Redis 黑名单并撤销 refreshToken 会话。
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        IssuedToken token = tokenService.refresh(request.refreshToken());
        return new LoginResponse(token.accessToken(), token.refreshToken(), token.tokenType(), token.expiresAt());
    }

    @Override
    public TokenIntrospectionResponse introspect(TokenIntrospectionRequest request) {
        // TODO 完整校验签名、过期时间、jti 黑名单和会话状态。
        return new TokenIntrospectionResponse(false, null, null, null, Collections.emptyList(), Collections.emptyList(), null, null);
    }

    @Override
    public CurrentUserResponse me(String authorization) {
        // TODO 从网关透传身份或 accessToken 解析结果获取当前用户。
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public String publicKey() {
        return keyProvider.getPublicKeyPem();
    }
}
