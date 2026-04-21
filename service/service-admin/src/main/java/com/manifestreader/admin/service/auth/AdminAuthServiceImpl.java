package com.manifestreader.admin.service.auth;

import com.manifestreader.admin.model.dto.AdminLoginRequest;
import com.manifestreader.admin.model.vo.AdminLoginResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthServiceImpl.class);

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        log.info("[admin-login-success] username={}, role={}", request.getUsername(), request.getRole());
        AdminLoginResponse response = new AdminLoginResponse();
        response.setUsername(request.getUsername());
        response.setRole(request.getRole());
        response.setAccessToken("admin-" + UUID.randomUUID());
        return response;
    }
}
