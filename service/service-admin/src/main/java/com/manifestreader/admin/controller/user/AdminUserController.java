package com.manifestreader.admin.controller.user;

import com.manifestreader.admin.model.dto.AdminUserRequest;
import com.manifestreader.admin.model.dto.AdminUserStatusRequest;
import com.manifestreader.admin.model.vo.AdminUserBillVO;
import com.manifestreader.admin.model.vo.AdminUserVO;
import com.manifestreader.admin.service.user.AdminUserService;
import com.manifestreader.common.result.Result;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public Result<List<AdminUserVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        log.info("[admin-user-list] keyword={}, status={}", keyword, status);
        return Result.success(adminUserService.listUsers(keyword, status));
    }

    @PostMapping("/users")
    public Result<AdminUserVO> createUser(@RequestBody AdminUserRequest request) {
        log.info("[admin-user-create] username={}, role={}", request.getUsername(), request.getRole());
        return Result.success(adminUserService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    public Result<AdminUserVO> updateUser(@PathVariable String userId, @RequestBody AdminUserRequest request) {
        log.info("[admin-user-update] userId={}, username={}, role={}", userId, request.getUsername(), request.getRole());
        return Result.success(adminUserService.updateUser(userId, request));
    }

    @PatchMapping("/users/{userId}/status")
    public Result<AdminUserVO> updateUserStatus(
            @PathVariable String userId,
            @RequestBody AdminUserStatusRequest request) {
        log.info("[admin-user-status] userId={}, status={}", userId, request.getStatus());
        return Result.success(adminUserService.updateUserStatus(userId, request.getStatus()));
    }

    @DeleteMapping("/users/{userId}")
    public Result<Void> deleteUser(@PathVariable String userId) {
        log.info("[admin-user-delete] userId={}", userId);
        adminUserService.deleteUser(userId);
        return Result.success();
    }

    @GetMapping("/users/{userId}/bills")
    public Result<List<AdminUserBillVO>> listUserBills(@PathVariable String userId) {
        log.info("[admin-user-bills] userId={}", userId);
        return Result.success(adminUserService.listUserBills(userId));
    }
}
