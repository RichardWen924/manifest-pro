package com.manifest.auth.controller;

import com.manifest.auth.dto.LoginRequest;
import com.manifest.auth.dto.RefreshTokenRequest;
import com.manifest.auth.dto.TokenIntrospectionRequest;
import com.manifest.auth.service.AuthService;
import com.manifest.auth.vo.CurrentUserResponse;
import com.manifest.auth.vo.LoginResponse;
import com.manifest.auth.vo.TokenIntrospectionResponse;
import com.manifestreader.common.constant.HeaderConstants;
import com.manifestreader.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证中心")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader(value = HeaderConstants.AUTHORIZATION, required = false) String authorization) {
        authService.logout(authorization);
        return R.ok();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/token/refresh")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return R.ok(authService.refresh(request));
    }

    @Operation(summary = "Token introspection")
    @PostMapping("/token/introspect")
    public R<TokenIntrospectionResponse> introspect(@Valid @RequestBody TokenIntrospectionRequest request) {
        return R.ok(authService.introspect(request));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public R<CurrentUserResponse> me(@RequestHeader(value = HeaderConstants.AUTHORIZATION, required = false) String authorization) {
        return R.ok(authService.me(authorization));
    }

    @Operation(summary = "获取 JWT 验签公钥")
    @GetMapping("/public-key")
    public R<String> publicKey() {
        return R.ok(authService.publicKey());
    }
}
