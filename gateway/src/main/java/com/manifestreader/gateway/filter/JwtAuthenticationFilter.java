package com.manifestreader.gateway.filter;

import com.manifestreader.common.constant.HeaderConstants;
import com.manifestreader.common.util.TraceIdUtil;
import com.manifestreader.gateway.properties.GatewaySecurityProperties;
import com.manifestreader.gateway.security.GatewayErrorWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GatewaySecurityProperties properties;
    private final GatewayErrorWriter errorWriter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private volatile ReactiveJwtDecoder jwtDecoder;

    public JwtAuthenticationFilter(GatewaySecurityProperties properties, GatewayErrorWriter errorWriter) {
        this.properties = properties;
        this.errorWriter = errorWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String traceId = ensureTraceId(exchange);
        if (isWhitelisted(path)) {
            return chain.filter(withTrace(exchange, traceId));
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HeaderConstants.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "401", "缺少访问令牌");
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (!looksLikeJwt(token)) {
            return errorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "401", "访问令牌格式错误");
        }

        return decoder().decode(token)
                .flatMap(jwt -> chain.filter(withIdentity(exchange, traceId, jwt)))
                .onErrorResume(ex -> errorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "401", "访问令牌无效或已过期"));
    }

    private ServerWebExchange withIdentity(ServerWebExchange exchange, String traceId, Jwt jwt) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HeaderConstants.TRACE_ID, traceId)
                .header(HeaderConstants.USER_ID, stringClaim(jwt, "userId", jwt.getSubject()))
                .header(HeaderConstants.COMPANY_ID, stringClaim(jwt, "companyId", ""))
                .header(HeaderConstants.USERNAME, stringClaim(jwt, "username", ""))
                .header(HeaderConstants.ROLE_CODES, String.join(",", listClaim(jwt, "roleCodes")))
                .build();
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhitelisted(String path) {
        return properties.getWhitelist().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean looksLikeJwt(String token) {
        return token != null && token.chars().filter(ch -> ch == '.').count() == 2;
    }

    private ReactiveJwtDecoder decoder() {
        ReactiveJwtDecoder local = jwtDecoder;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (jwtDecoder == null) {
                jwtDecoder = NimbusReactiveJwtDecoder.withPublicKey(readPublicKey()).build();
            }
            return jwtDecoder;
        }
    }

    private RSAPublicKey readPublicKey() {
        try {
            String pem = Files.readString(Path.of(properties.getPublicKeyPath()), StandardCharsets.UTF_8);
            String body = pem
                    .replaceAll("-----BEGIN [A-Z ]+-----", "")
                    .replaceAll("-----END [A-Z ]+-----", "")
                    .replaceAll("\\s", "");
            byte[] bytes = Base64.getDecoder().decode(body);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("Gateway JWT public key is not readable: " + properties.getPublicKeyPath(), ex);
        }
    }

    private String stringClaim(Jwt jwt, String claimName, String fallback) {
        Object value = jwt.getClaim(claimName);
        return value == null ? fallback : value.toString();
    }

    private List<String> listClaim(Jwt jwt, String claimName) {
        List<String> values = jwt.getClaimAsStringList(claimName);
        return values == null ? List.of() : values;
    }

    private String ensureTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(HeaderConstants.TRACE_ID);
        return traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId;
    }

    private ServerWebExchange withTrace(ServerWebExchange exchange, String traceId) {
        TraceIdUtil.setTraceId(traceId);
        ServerHttpRequest request = exchange.getRequest().mutate().header(HeaderConstants.TRACE_ID, traceId).build();
        return exchange.mutate().request(request).build();
    }
}
