package com.manifestreader.admin.service.auth;

import com.manifestreader.admin.model.dto.AdminLoginRequest;
import com.manifestreader.admin.model.vo.AdminLoginResponse;
import com.manifestreader.common.exception.BizException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAuthServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminLoginUser loginUser = findLoginUser(request.getUsername());
        if (loginUser == null || !passwordEncoder.matches(request.getPassword(), loginUser.passwordHash())) {
            log.warn("[admin-login-failed] username={}, reason=bad_credentials", request.getUsername());
            throw new BizException("UNAUTHORIZED", "账号或密码错误");
        }
        if (loginUser.status() == null || loginUser.status() != 1) {
            log.warn("[admin-login-failed] username={}, reason=disabled", request.getUsername());
            throw new BizException("FORBIDDEN", "账号已被禁用");
        }

        String role = findPrimaryRoleCode(loginUser.id());
        log.info("[admin-login-success] username={}, role={}", loginUser.username(), role);
        AdminLoginResponse response = new AdminLoginResponse();
        response.setUsername(loginUser.username());
        response.setRole(role);
        response.setAccessToken("admin-" + UUID.randomUUID());
        return response;
    }

    private AdminLoginUser findLoginUser(String username) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                            SELECT id, username, password_hash, status
                            FROM sys_user
                            WHERE username = ? AND deleted = 0
                            LIMIT 1
                            """,
                    (rs, rowNum) -> new AdminLoginUser(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getInt("status")
                    ),
                    username
            );
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private String findPrimaryRoleCode(Long userId) {
        return jdbcTemplate.query(
                """
                        SELECT r.role_code
                        FROM sys_user_role ur
                        JOIN sys_role r ON r.id = ur.role_id
                        WHERE ur.user_id = ? AND r.status = 1
                        ORDER BY r.id
                        LIMIT 1
                        """,
                rs -> rs.next() ? rs.getString("role_code") : "TENANT_USER",
                userId
        );
    }

    private record AdminLoginUser(Long id, String username, String passwordHash, Integer status) {
    }
}
