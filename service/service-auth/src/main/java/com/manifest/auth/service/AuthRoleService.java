package com.manifest.auth.service;

import com.manifest.auth.dto.RoleCreateRequest;
import com.manifest.auth.dto.RolePermissionAssignRequest;
import com.manifest.auth.dto.RoleUpdateRequest;
import com.manifest.auth.vo.PermissionTreeVO;
import com.manifest.auth.vo.RoleVO;
import java.util.List;

public interface AuthRoleService {

    List<RoleVO> listRoles();

    RoleVO createRole(RoleCreateRequest request);

    RoleVO updateRole(Long id, RoleUpdateRequest request);

    List<PermissionTreeVO> permissionTree();

    void assignPermissions(Long id, RolePermissionAssignRequest request);
}
