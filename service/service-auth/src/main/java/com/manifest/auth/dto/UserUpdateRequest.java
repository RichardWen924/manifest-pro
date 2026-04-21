package com.manifest.auth.dto;

public record UserUpdateRequest(
        String nickname,
        String mobile,
        String email,
        Integer status,
        String remark
) {
}
