package com.manifest.auth.service.impl;

import com.manifest.auth.dto.UserCreateRequest;
import com.manifest.auth.dto.UserPageQuery;
import com.manifest.auth.dto.UserRoleAssignRequest;
import com.manifest.auth.dto.UserStatusRequest;
import com.manifest.auth.dto.UserUpdateRequest;
import com.manifest.auth.service.AuthUserService;
import com.manifest.auth.vo.UserVO;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.PageResult;
import org.springframework.stereotype.Service;

@Service
public class AuthUserServiceImpl implements AuthUserService {

    @Override
    public PageResult<UserVO> page(UserPageQuery query) {
        return PageResult.empty(query.pageNo(), query.pageSize());
    }

    @Override
    public UserVO detail(Long id) {
        return new UserVO(id, null, null, null, null, null, null, java.util.Collections.emptyList(), null);
    }

    @Override
    public UserVO create(UserCreateRequest request) {
        // TODO 使用 BCrypt 加密密码后写入 sys_user，禁止返回密码字段。
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public UserVO update(Long id, UserUpdateRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public void updateStatus(Long id, UserStatusRequest request) {
        // TODO 更新 sys_user.status。
    }

    @Override
    public void assignRoles(Long id, UserRoleAssignRequest request) {
        // TODO 维护 sys_user_role。
    }
}
