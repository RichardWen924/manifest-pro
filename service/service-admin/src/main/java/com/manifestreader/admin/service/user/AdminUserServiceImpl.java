package com.manifestreader.admin.service.user;

import com.manifestreader.admin.model.dto.AdminUserRequest;
import com.manifestreader.admin.model.vo.AdminUserBillVO;
import com.manifestreader.admin.model.vo.AdminUserVO;
import com.manifestreader.common.exception.BizException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final AtomicLong idSequence = new AtomicLong(1004);
    private final Map<String, AdminUserVO> users = new ConcurrentHashMap<>();
    private final Map<String, List<AdminUserBillVO>> bills = new ConcurrentHashMap<>();

    public AdminUserServiceImpl() {
        seedUsers();
        seedBills();
    }

    @Override
    public List<AdminUserVO> listUsers(String keyword, String status) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        String normalizedStatus = status == null ? "" : status.trim();

        return users.values().stream()
                .filter(user -> normalizedKeyword.isBlank() || containsKeyword(user, normalizedKeyword))
                .filter(user -> normalizedStatus.isBlank() || normalizedStatus.equals(user.getStatus()))
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .toList();
    }

    @Override
    public AdminUserVO createUser(AdminUserRequest request) {
        AdminUserVO user = toUserVO("u-" + idSequence.getAndIncrement(), request);
        users.put(user.getId(), user);
        bills.putIfAbsent(user.getId(), Collections.emptyList());
        log.info("[admin-user-created] userId={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    @Override
    public AdminUserVO updateUser(String userId, AdminUserRequest request) {
        ensureUserExists(userId);
        AdminUserVO user = toUserVO(userId, request);
        users.put(userId, user);
        log.info("[admin-user-updated] userId={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    @Override
    public AdminUserVO updateUserStatus(String userId, String status) {
        AdminUserVO user = ensureUserExists(userId);
        user.setStatus(status);
        users.put(userId, user);
        log.info("[admin-user-status-updated] userId={}, status={}", userId, status);
        return user;
    }

    @Override
    public void deleteUser(String userId) {
        ensureUserExists(userId);
        users.remove(userId);
        bills.remove(userId);
        log.info("[admin-user-deleted] userId={}", userId);
    }

    @Override
    public List<AdminUserBillVO> listUserBills(String userId) {
        ensureUserExists(userId);
        return bills.getOrDefault(userId, Collections.emptyList());
    }

    private void seedUsers() {
        addSeedUser("u-1001", "admin", "平台管理员", "admin@manifest.local", "13800000001", "平台管理员", "enabled");
        addSeedUser("u-1002", "tenant_ops", "企业操作员", "ops@manifest.local", "13800000002", "业务操作员", "enabled");
        addSeedUser("u-1003", "auditor", "审计员", "audit@manifest.local", "13800000003", "企业管理员", "disabled");
    }

    private void seedBills() {
        bills.put("u-1001", List.of(
                bill("MRBL240001", "COSCO Star / 042E", "Shanghai", "Los Angeles", "已确认"),
                bill("MRBL240002", "Ever Bloom / 118W", "Ningbo", "Hamburg", "待确认")));
        bills.put("u-1002", List.of(
                bill("MRBL240003", "OOCL Asia / 063E", "Qingdao", "Singapore", "解析中")));
        bills.put("u-1003", List.of(
                bill("MRBL240004", "Maersk Pearl / 221A", "Xiamen", "Rotterdam", "待确认")));
    }

    private void addSeedUser(
            String id,
            String username,
            String nickname,
            String email,
            String mobile,
            String role,
            String status) {
        AdminUserVO user = new AdminUserVO();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setRole(role);
        user.setStatus(status);
        users.put(id, user);
    }

    private AdminUserVO toUserVO(String id, AdminUserRequest request) {
        AdminUserVO user = new AdminUserVO();
        user.setId(id);
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setMobile(request.getMobile());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());
        return user;
    }

    private AdminUserVO ensureUserExists(String userId) {
        AdminUserVO user = users.get(userId);
        if (user == null) {
            throw new BizException("USER_NOT_FOUND", "User not found: " + userId);
        }
        return user;
    }

    private boolean containsKeyword(AdminUserVO user, String keyword) {
        List<String> values = new ArrayList<>();
        values.add(user.getUsername());
        values.add(user.getNickname());
        values.add(user.getEmail());
        values.add(user.getMobile());
        return values.stream()
                .filter(value -> value != null)
                .map(String::toLowerCase)
                .anyMatch(value -> value.contains(keyword));
    }

    private AdminUserBillVO bill(String blNo, String vesselVoyage, String pol, String pod, String status) {
        AdminUserBillVO bill = new AdminUserBillVO();
        bill.setBlNo(blNo);
        bill.setVesselVoyage(vesselVoyage);
        bill.setPol(pol);
        bill.setPod(pod);
        bill.setStatus(status);
        return bill;
    }
}
