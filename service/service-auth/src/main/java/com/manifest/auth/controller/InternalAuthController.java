package com.manifest.auth.controller;

import com.manifest.auth.service.InternalIdentityService;
import com.manifestreader.api.auth.dto.PermissionDTO;
import com.manifestreader.api.auth.dto.TokenIntrospectionDTO;
import com.manifestreader.api.auth.dto.TokenIntrospectionRequestDTO;
import com.manifestreader.api.auth.dto.UserIdentityDTO;
import com.manifestreader.common.result.R;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final InternalIdentityService identityService;

    public InternalAuthController(InternalIdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping("/users/{id}/identity")
    public R<UserIdentityDTO> getUserIdentity(@PathVariable Long id) {
        return R.ok(identityService.getUserIdentity(id));
    }

    @PostMapping("/token/introspect")
    public R<TokenIntrospectionDTO> introspect(@RequestBody TokenIntrospectionRequestDTO request) {
        return R.ok(identityService.introspect(request));
    }

    @GetMapping("/users/{id}/permissions")
    public R<List<PermissionDTO>> getUserPermissions(@PathVariable Long id) {
        return R.ok(identityService.getUserPermissions(id));
    }
}
