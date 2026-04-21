package com.manifestreader.admin.controller.auth;

import com.manifestreader.admin.model.dto.AdminLoginRequest;
import com.manifestreader.admin.model.vo.AdminLoginResponse;
import com.manifestreader.admin.service.auth.AdminAuthService;
import com.manifestreader.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAuthController {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthController.class);

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        log.info("[admin-login] username={}, role={}", request.getUsername(), request.getRole());
        return Result.success(adminAuthService.login(request));
    }
}
