package com.manifest.auth.service;

import com.manifestreader.api.auth.dto.PermissionDTO;
import com.manifestreader.api.auth.dto.TokenIntrospectionDTO;
import com.manifestreader.api.auth.dto.TokenIntrospectionRequestDTO;
import com.manifestreader.api.auth.dto.UserIdentityDTO;
import java.util.List;

public interface InternalIdentityService {

    UserIdentityDTO getUserIdentity(Long id);

    TokenIntrospectionDTO introspect(TokenIntrospectionRequestDTO request);

    List<PermissionDTO> getUserPermissions(Long id);
}
