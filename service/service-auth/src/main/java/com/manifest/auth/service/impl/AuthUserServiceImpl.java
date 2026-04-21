package com.manifest.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.manifest.auth.dto.UserCreateRequest;
import com.manifest.auth.dto.UserPageQuery;
import com.manifest.auth.dto.UserRoleAssignRequest;
import com.manifest.auth.dto.UserStatusRequest;
import com.manifest.auth.dto.UserUpdateRequest;
import com.manifest.auth.mapper.RoleMapper;
import com.manifest.auth.mapper.UserMapper;
import com.manifest.auth.mapper.UserRoleMapper;
import com.manifest.auth.service.AuthUserService;
import com.manifest.auth.vo.UserVO;
import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.model.auth.entity.UserRoleEntity;
import com.manifestreader.model.entity.RoleEntity;
import com.manifestreader.model.entity.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthUserServiceImpl implements AuthUserService {

    private static final String DEFAULT_ROLE_CODE = "TENANT_USER";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthUserServiceImpl(UserMapper userMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PageResult<UserVO> page(UserPageQuery query) {
        return PageResult.empty(query.pageNo(), query.pageSize());
    }

    @Override
    public UserVO detail(Long id) {
        return new UserVO(id, null, null, null, null, null, null, java.util.Collections.emptyList(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO create(UserCreateRequest request) {
        if (existsUsername(request.username())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        UserEntity entity = new UserEntity();
        entity.setCompanyId(request.companyId());
        entity.setUsername(request.username());
        entity.setPasswordHash(passwordEncoder.encode(request.password()));
        entity.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname() : request.username());
        entity.setMobile(request.mobile());
        entity.setEmail(request.email());
        entity.setStatus(request.status() == null ? 1 : request.status());
        entity.setDeleted(0);

        try {
            userMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "用户名已存在");
        }

        assignDefaultRole(entity.getId());
        return toVO(entity, List.of(DEFAULT_ROLE_CODE));
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

    private boolean existsUsername(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getDeleted, 0)) > 0;
    }

    private void assignDefaultRole(Long userId) {
        RoleEntity role = roleMapper.selectOne(new LambdaQueryWrapper<RoleEntity>()
                .eq(RoleEntity::getRoleCode, DEFAULT_ROLE_CODE)
                .last("LIMIT 1"));
        if (role == null) {
            return;
        }
        UserRoleEntity relation = new UserRoleEntity();
        relation.setUserId(userId);
        relation.setRoleId(role.getId());
        relation.setCreatedAt(LocalDateTime.now());
        userRoleMapper.insert(relation);
    }

    private UserVO toVO(UserEntity entity, List<String> roleCodes) {
        return new UserVO(
                entity.getId(),
                entity.getCompanyId(),
                entity.getUsername(),
                entity.getNickname(),
                entity.getMobile(),
                entity.getEmail(),
                entity.getStatus(),
                roleCodes,
                entity.getCreatedAt()
        );
    }
}
