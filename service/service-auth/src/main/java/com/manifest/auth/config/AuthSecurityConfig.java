package com.manifest.auth.config;

import com.manifest.auth.properties.AuthTokenProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(AuthTokenProperties.class)
public class AuthSecurityConfig {

    @Bean
    public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/auth/login",
                                "/auth/users",
                                "/auth/token/refresh",
                                "/auth/token/introspect",
                                "/auth/public-key",
                                "/internal/auth/token/introspect",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
