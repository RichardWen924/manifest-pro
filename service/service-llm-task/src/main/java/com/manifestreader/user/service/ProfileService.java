package com.manifestreader.user.service;

import com.manifestreader.user.model.dto.PasswordChangeRequest;
import com.manifestreader.user.model.dto.ProfileUpdateRequest;
import com.manifestreader.user.model.vo.ProfileVO;

public interface ProfileService {

    ProfileVO current();

    ProfileVO update(ProfileUpdateRequest request);

    void changePassword(PasswordChangeRequest request);
}
