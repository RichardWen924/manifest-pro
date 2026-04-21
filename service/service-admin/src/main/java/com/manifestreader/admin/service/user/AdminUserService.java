package com.manifestreader.admin.service.user;

import com.manifestreader.admin.model.dto.AdminUserRequest;
import com.manifestreader.admin.model.vo.AdminUserBillVO;
import com.manifestreader.admin.model.vo.AdminUserVO;
import java.util.List;

public interface AdminUserService {

    List<AdminUserVO> listUsers(String keyword, String status);

    AdminUserVO createUser(AdminUserRequest request);

    AdminUserVO updateUser(String userId, AdminUserRequest request);

    AdminUserVO updateUserStatus(String userId, String status);

    void deleteUser(String userId);

    List<AdminUserBillVO> listUserBills(String userId);
}
