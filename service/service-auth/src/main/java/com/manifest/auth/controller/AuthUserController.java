package com.manifest.auth.controller;

import com.manifest.auth.dto.UserCreateRequest;
import com.manifest.auth.dto.UserPageQuery;
import com.manifest.auth.dto.UserRoleAssignRequest;
import com.manifest.auth.dto.UserStatusRequest;
import com.manifest.auth.dto.UserUpdateRequest;
import com.manifest.auth.service.AuthUserService;
import com.manifest.auth.vo.UserVO;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证中心-用户")
@RestController
@RequestMapping("/auth/users")
public class AuthUserController {

    private final AuthUserService userService;

    public AuthUserController(AuthUserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户分页")
    @GetMapping("/page")
    public R<PageResult<UserVO>> page(UserPageQuery query) {
        return R.ok(userService.page(query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public R<UserVO> detail(@PathVariable Long id) {
        return R.ok(userService.detail(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public R<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return R.ok(userService.create(request));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public R<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return R.ok(userService.update(id, request));
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        userService.updateStatus(id, request);
        return R.ok();
    }

    @Operation(summary = "分配用户角色")
    @PostMapping("/{id}/roles")
    public R<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody UserRoleAssignRequest request) {
        userService.assignRoles(id, request);
        return R.ok();
    }
}
