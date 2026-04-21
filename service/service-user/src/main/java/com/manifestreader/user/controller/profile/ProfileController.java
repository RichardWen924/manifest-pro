package com.manifestreader.user.controller.profile;

import com.manifestreader.common.result.R;
import com.manifestreader.user.model.dto.PasswordChangeRequest;
import com.manifestreader.user.model.dto.ProfileUpdateRequest;
import com.manifestreader.user.model.vo.ProfileVO;
import com.manifestreader.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户端-个人资料")
@RestController
@RequestMapping("/user/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "当前用户资料")
    @GetMapping
    public R<ProfileVO> current() {
        return R.ok(profileService.current());
    }

    @Operation(summary = "更新当前用户资料")
    @PutMapping
    public R<ProfileVO> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return R.ok(profileService.update(request));
    }

    @Operation(summary = "修改当前用户密码")
    @PutMapping("/password")
    public R<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        profileService.changePassword(request);
        return R.ok();
    }
}
