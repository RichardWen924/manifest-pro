package com.manifestreader.user.feign;

import com.manifestreader.api.auth.dto.PermissionDTO;
import com.manifestreader.api.auth.dto.UserIdentityDTO;
import com.manifestreader.common.result.R;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "manifest-reader-auth", contextId = "userAuthIdentityFeignClient")
public interface AuthIdentityFeignClient {

    @GetMapping("/internal/auth/users/{id}/identity")
    R<UserIdentityDTO> getUserIdentity(@PathVariable("id") Long id);

    @GetMapping("/internal/auth/users/{id}/permissions")
    R<List<PermissionDTO>> getUserPermissions(@PathVariable("id") Long id);
}
