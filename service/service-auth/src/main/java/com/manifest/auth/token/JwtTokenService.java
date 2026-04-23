package com.manifest.auth.token;

import com.manifest.auth.properties.AuthTokenProperties;
import com.manifest.auth.security.KeyProvider;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

@Service
public class JwtTokenService implements TokenService {

    private final AuthTokenProperties properties;
    private final KeyProvider keyProvider;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final Map<String, TokenPrincipal> refreshSessions = new ConcurrentHashMap<>();
    private final Map<String, Instant> revokedJtis = new ConcurrentHashMap<>();

    public JwtTokenService(AuthTokenProperties properties, KeyProvider keyProvider) {
        this.properties = properties;
        this.keyProvider = keyProvider;
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyProvider.getPublicKey())
                .privateKey((RSAPrivateKey) keyProvider.getPrivateKey())
                .keyID("manifest-reader-auth")
                .build();
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new com.nimbusds.jose.jwk.JWKSet(rsaKey)));
        this.jwtDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyProvider.getPublicKey()).build();
    }

    @Override
    public IssuedToken issue(TokenPrincipal principal) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getAccessTokenTtl());
        String jti = principal.jti() == null ? UUID.randomUUID().toString() : principal.jti();
        TokenPrincipal issuedPrincipal = new TokenPrincipal(
                principal.userId(),
                principal.companyId(),
                principal.username(),
                safeList(principal.roleCodes()),
                safeList(principal.permissionCodes()),
                jti,
                now,
                expiresAt
        );
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(String.valueOf(issuedPrincipal.userId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .id(jti)
                .claim("userId", issuedPrincipal.userId())
                .claim("companyId", issuedPrincipal.companyId())
                .claim("username", issuedPrincipal.username())
                .claim("roleCodes", issuedPrincipal.roleCodes())
                .claim("permissions", issuedPrincipal.permissionCodes())
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims
        )).getTokenValue();
        String refreshToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        refreshSessions.put(refreshToken, issuedPrincipal);
        return new IssuedToken(accessToken, refreshToken, "Bearer", jti, expiresAt);
    }

    @Override
    public TokenPrincipal parse(String accessToken) {
        try {
            Jwt jwt = jwtDecoder.decode(accessToken);
            String jti = jwt.getId();
            if (jti != null && revokedJtis.containsKey(jti)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "访问令牌已失效");
            }
            return new TokenPrincipal(
                    asLong(jwt.getClaim("userId"), jwt.getSubject()),
                    asLong(jwt.getClaim("companyId"), null),
                    jwt.getClaimAsString("username"),
                    jwt.getClaimAsStringList("roleCodes") == null ? List.of() : jwt.getClaimAsStringList("roleCodes"),
                    jwt.getClaimAsStringList("permissions") == null ? List.of() : jwt.getClaimAsStringList("permissions"),
                    jti,
                    jwt.getIssuedAt(),
                    jwt.getExpiresAt()
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (JwtException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "访问令牌无效或已过期");
        }
    }

    @Override
    public IssuedToken refresh(String refreshToken) {
        TokenPrincipal principal = refreshSessions.remove(refreshToken);
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "刷新令牌无效或已过期");
        }
        if (principal.issuedAt() != null && principal.issuedAt().plus(properties.getRefreshTokenTtl()).isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "刷新令牌已过期");
        }
        return issue(new TokenPrincipal(
                principal.userId(),
                principal.companyId(),
                principal.username(),
                principal.roleCodes(),
                principal.permissionCodes(),
                null,
                null,
                null
        ));
    }

    @Override
    public void revoke(String jti) {
        if (jti != null && !jti.isBlank()) {
            revokedJtis.put(jti, Instant.now().plus(properties.getAccessTokenTtl()));
        }
    }

    @Override
    public boolean validate(String accessToken) {
        try {
            parse(accessToken);
            return true;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private Long asLong(Object value, String fallback) {
        Object candidate = value == null ? fallback : value;
        if (candidate == null) {
            return null;
        }
        if (candidate instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(candidate.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
