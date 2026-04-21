package com.manifest.auth.token;

import com.manifest.auth.properties.AuthTokenProperties;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService implements TokenService {

    private final AuthTokenProperties properties;

    public JwtTokenService(AuthTokenProperties properties) {
        this.properties = properties;
    }

    @Override
    public IssuedToken issue(TokenPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getAccessTokenTtl());
        String jti = principal.jti() == null ? UUID.randomUUID().toString() : principal.jti();
        // TODO 使用 Nimbus JOSE JWT + RSA 私钥签发真实 JWT，并写入 refreshToken 会话。
        return new IssuedToken("access-token-placeholder", UUID.randomUUID().toString(), "Bearer", jti, expiresAt);
    }

    @Override
    public TokenPrincipal parse(String accessToken) {
        // TODO 使用 RSA 公钥解析 JWT Claims，并校验 jti 黑名单。
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public IssuedToken refresh(String refreshToken) {
        // TODO 校验 opaque refreshToken、轮换会话并重新签发 accessToken。
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public void revoke(String jti) {
        // TODO 将 jti 写入 Redis 黑名单并撤销 sys_token_session。
    }

    @Override
    public boolean validate(String accessToken) {
        // TODO 接入 Nimbus JWT 验签、过期校验和 Redis 黑名单。
        return false;
    }
}
