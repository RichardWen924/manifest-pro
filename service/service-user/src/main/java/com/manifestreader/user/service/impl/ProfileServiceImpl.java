package com.manifestreader.user.service.impl;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.user.model.dto.PasswordChangeRequest;
import com.manifestreader.user.model.dto.ProfileUpdateRequest;
import com.manifestreader.user.model.vo.ProfileVO;
import com.manifestreader.user.service.ProfileService;
import java.util.Collections;
import org.springframework.stereotype.Service;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Override
    public ProfileVO current() {
        return new ProfileVO(null, null, null, null, null, null, Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public ProfileVO update(ProfileUpdateRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public void changePassword(PasswordChangeRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }
}
