package com.manifest.auth.service.impl;

import com.manifest.auth.dto.RoleCreateRequest;
import com.manifest.auth.dto.RolePermissionAssignRequest;
import com.manifest.auth.dto.RoleUpdateRequest;
import com.manifest.auth.service.AuthRoleService;
import com.manifest.auth.vo.PermissionTreeVO;
import com.manifest.auth.vo.RoleVO;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuthRoleServiceImpl implements AuthRoleService {

    @Override
    public List<RoleVO> listRoles() {
        return Collections.emptyList();
    }

    @Override
    public RoleVO createRole(RoleCreateRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public RoleVO updateRole(Long id, RoleUpdateRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public List<PermissionTreeVO> permissionTree() {
        // TODO 后续实现权限树装配，当前只保留接口形态。
        return Collections.emptyList();
    }

    @Override
    public void assignPermissions(Long id, RolePermissionAssignRequest request) {
        // TODO 维护 sys_role_permission。
    }
}
