package com.manifestreader.admin.service.auth;

import com.manifestreader.admin.model.dto.AdminLoginRequest;
import com.manifestreader.admin.model.vo.AdminLoginResponse;

public interface AdminAuthService {

    AdminLoginResponse login(AdminLoginRequest request);
}
