package com.manifest.auth.controller;

import com.manifest.auth.dto.RoleCreateRequest;
import com.manifest.auth.dto.RolePermissionAssignRequest;
import com.manifest.auth.dto.RoleUpdateRequest;
import com.manifest.auth.service.AuthRoleService;
import com.manifest.auth.vo.PermissionTreeVO;
import com.manifest.auth.vo.RoleVO;
import com.manifestreader.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证中心-角色权限")
@RestController
@RequestMapping("/auth")
public class AuthRoleController {

    private final AuthRoleService roleService;

    public AuthRoleController(AuthRoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "角色列表")
    @GetMapping("/roles/list")
    public R<List<RoleVO>> listRoles() {
        return R.ok(roleService.listRoles());
    }

    @Operation(summary = "创建角色")
    @PostMapping("/roles")
    public R<RoleVO> createRole(@Valid @RequestBody RoleCreateRequest request) {
        return R.ok(roleService.createRole(request));
    }

    @Operation(summary = "更新角色")
    @PutMapping("/roles/{id}")
    public R<RoleVO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return R.ok(roleService.updateRole(id, request));
    }

    @Operation(summary = "权限树")
    @GetMapping("/permissions/tree")
    public R<List<PermissionTreeVO>> permissionTree() {
        return R.ok(roleService.permissionTree());
    }

    @Operation(summary = "分配角色权限")
    @PostMapping("/roles/{id}/permissions")
    public R<Void> assignPermissions(@PathVariable Long id, @Valid @RequestBody RolePermissionAssignRequest request) {
        roleService.assignPermissions(id, request);
        return R.ok();
    }
}
