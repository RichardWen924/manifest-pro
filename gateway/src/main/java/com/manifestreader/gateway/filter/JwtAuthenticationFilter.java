package com.manifestreader.gateway.filter;

import com.manifestreader.common.constant.HeaderConstants;
import com.manifestreader.common.util.TraceIdUtil;
import com.manifestreader.gateway.properties.GatewaySecurityProperties;
import com.manifestreader.gateway.security.GatewayErrorWriter;
import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
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

        // TODO 使用 NimbusJwtDecoder 加载 auth-service 公钥，完成 JWT 签名、exp、jti 黑名单校验。
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(HeaderConstants.TRACE_ID, traceId)
                .header(HeaderConstants.USER_ID, "")
                .header(HeaderConstants.COMPANY_ID, "")
                .header(HeaderConstants.USERNAME, "")
                .header(HeaderConstants.ROLE_CODES, "")
                .build();
        return chain.filter(exchange.mutate().request(request).build());
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
