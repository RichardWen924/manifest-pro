package com.manifest.auth.service;

import com.manifest.auth.dto.UserCreateRequest;
import com.manifest.auth.dto.UserPageQuery;
import com.manifest.auth.dto.UserRoleAssignRequest;
import com.manifest.auth.dto.UserStatusRequest;
import com.manifest.auth.dto.UserUpdateRequest;
import com.manifest.auth.vo.UserVO;
import com.manifestreader.common.result.PageResult;

public interface AuthUserService {

    PageResult<UserVO> page(UserPageQuery query);

    UserVO detail(Long id);

    UserVO create(UserCreateRequest request);

    UserVO update(Long id, UserUpdateRequest request);

    void updateStatus(Long id, UserStatusRequest request);

    void assignRoles(Long id, UserRoleAssignRequest request);
}
