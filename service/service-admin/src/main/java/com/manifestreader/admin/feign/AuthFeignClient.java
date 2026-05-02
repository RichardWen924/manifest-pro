package com.manifestreader.admin.feign;

import com.manifestreader.api.auth.dto.PermissionDTO;
import com.manifestreader.api.auth.dto.TokenIntrospectionDTO;
import com.manifestreader.api.auth.dto.TokenIntrospectionRequestDTO;
import com.manifestreader.api.auth.dto.UserIdentityDTO;
import com.manifestreader.common.result.R;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "manifest-reader-auth", contextId = "adminAuthFeignClient")
public interface AuthFeignClient {

    @GetMapping("/internal/auth/users/{id}/identity")
    R<UserIdentityDTO> getUserIdentity(@PathVariable("id") Long id);

    @PostMapping("/internal/auth/token/introspect")
    R<TokenIntrospectionDTO> introspect(@RequestBody TokenIntrospectionRequestDTO request);

    @GetMapping("/internal/auth/users/{id}/permissions")
    R<List<PermissionDTO>> getUserPermissions(@PathVariable("id") Long id);
}
