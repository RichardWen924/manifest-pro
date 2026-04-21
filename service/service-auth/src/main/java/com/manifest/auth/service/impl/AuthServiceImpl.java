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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final TokenService tokenService;
    private final KeyProvider keyProvider;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public AuthServiceImpl(TokenService tokenService, KeyProvider keyProvider, PasswordEncoder passwordEncoder, JdbcTemplate jdbcTemplate) {
        this.tokenService = tokenService;
        this.keyProvider = keyProvider;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        LoginUser loginUser = findLoginUser(request.identity(), request.username(), request.tenantCode(), request.password());
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "账号或密码错误");
        }
        if (loginUser.status() == null || loginUser.status() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "账号已被禁用");
        }

        List<String> roleCodes = findRoleCodes(loginUser.userId());
        IssuedToken token = tokenService.issue(new TokenPrincipal(
                loginUser.userId(),
                loginUser.companyId(),
                loginUser.username(),
                roleCodes,
                Collections.emptyList(),
                null,
                null,
                null
        ));
        return new LoginResponse(
                token.accessToken(),
                token.refreshToken(),
                token.tokenType(),
                token.expiresAt(),
                loginUser.userId(),
                loginUser.companyId(),
                loginUser.username(),
                roleCodes
        );
    }

    @Override
    public void logout(String authorization) {
        // TODO 解析 accessToken jti，写入 Redis 黑名单并撤销 refreshToken 会话。
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        IssuedToken token = tokenService.refresh(request.refreshToken());
        return new LoginResponse(token.accessToken(), token.refreshToken(), token.tokenType(), token.expiresAt(), null, null, null, Collections.emptyList());
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

    private LoginUser findLoginUser(String identity, String username, String tenantCode, String password) {
        String loginIdentity = hasText(identity) ? identity : username;
        String sql = """
                SELECT u.id, u.company_id, u.username, u.password_hash, u.status
                FROM sys_user u
                JOIN sys_company c ON c.id = u.company_id
                WHERE (u.username = ? OR c.company_code = ? OR c.company_abbr = ?)
                  AND u.deleted = 0
                  AND c.deleted = 0
                  AND (? IS NULL OR ? = '' OR c.company_code = ? OR c.company_abbr = ?)
                ORDER BY CASE WHEN u.username = ? THEN 0 ELSE 1 END, u.id
                """;
        try {
            List<LoginUser> candidates = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> new LoginUser(
                            rs.getLong("id"),
                            rs.getLong("company_id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getInt("status")
                    ),
                    loginIdentity,
                    loginIdentity,
                    loginIdentity,
                    tenantCode,
                    tenantCode,
                    tenantCode,
                    tenantCode,
                    loginIdentity
            );
            return candidates.stream()
                    .filter(candidate -> passwordEncoder.matches(password, candidate.passwordHash()))
                    .findFirst()
                    .orElse(null);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<String> findRoleCodes(Long userId) {
        return jdbcTemplate.query(
                """
                        SELECT r.role_code
                        FROM sys_user_role ur
                        JOIN sys_role r ON r.id = ur.role_id
                        WHERE ur.user_id = ? AND r.status = 1
                        ORDER BY r.id
                        """,
                rs -> {
                    List<String> roleCodes = new ArrayList<>();
                    while (rs.next()) {
                        roleCodes.add(rs.getString("role_code"));
                    }
                    return roleCodes;
                },
                userId
        );
    }

    private record LoginUser(Long userId, Long companyId, String username, String passwordHash, Integer status) {
    }
}
