package com.manifest.auth.service.impl;

import com.manifest.auth.service.InternalIdentityService;
import com.manifestreader.api.auth.dto.PermissionDTO;
import com.manifestreader.api.auth.dto.TokenIntrospectionDTO;
import com.manifestreader.api.auth.dto.TokenIntrospectionRequestDTO;
import com.manifestreader.api.auth.dto.UserIdentityDTO;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InternalIdentityServiceImpl implements InternalIdentityService {

    @Override
    public UserIdentityDTO getUserIdentity(Long id) {
        return new UserIdentityDTO(id, null, null, null, Collections.emptyList(), Collections.emptyList(), null);
    }

    @Override
    public TokenIntrospectionDTO introspect(TokenIntrospectionRequestDTO request) {
        return new TokenIntrospectionDTO(false, null, null, null, Collections.emptyList(), Collections.emptyList(), null, null);
    }

    @Override
    public List<PermissionDTO> getUserPermissions(Long id) {
        return Collections.emptyList();
    }
}
