package com.manifestreader.user.model.dto;

public record ProfileUpdateRequest(
        String nickname,
        String mobile,
        String email
) {
}
