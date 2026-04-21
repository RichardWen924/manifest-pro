-- =========================================================
-- Manifest Reader Refactor Seed Data
-- Target database: manifest_refactor
-- =========================================================

USE `manifest_refactor`;

SET NAMES utf8mb4;

-- Roles
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `role_scope`, `status`, `remark`)
VALUES
  (1, 'SUPER_ADMIN', '平台超级管理员', 'SYSTEM', 1, 'Platform super administrator'),
  (2, 'TENANT_ADMIN', '租户管理员', 'ADMIN', 1, 'Tenant admin'),
  (3, 'TENANT_USER', '租户用户', 'USER', 1, 'Tenant user')
ON DUPLICATE KEY UPDATE
  `role_name` = VALUES(`role_name`),
  `role_scope` = VALUES(`role_scope`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`);

-- Companies
INSERT INTO `sys_company`
(`id`, `company_code`, `company_name`, `company_abbr`, `status`, `vip_status`, `package_type`, `expire_at`, `remark`)
VALUES
  (1, 'PLATFORM', '平台运营中心', 'PLAT', 1, 1, 'SYSTEM', '2099-12-31 23:59:59', 'System tenant'),
  (2, 'TEST001', '测试公司', 'TEST', 1, 0, 'BASIC', DATE_ADD(NOW(), INTERVAL 30 DAY), 'Demo tenant')
ON DUPLICATE KEY UPDATE
  `company_name` = VALUES(`company_name`),
  `company_abbr` = VALUES(`company_abbr`),
  `status` = VALUES(`status`),
  `vip_status` = VALUES(`vip_status`),
  `package_type` = VALUES(`package_type`),
  `expire_at` = VALUES(`expire_at`),
  `remark` = VALUES(`remark`);

-- Users
-- password = test123 (BCrypt hash from legacy project)
INSERT INTO `sys_user`
(`id`, `company_id`, `username`, `password_hash`, `nickname`, `mobile`, `email`, `status`, `remark`)
VALUES
  (1, 1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbDr.i', '平台管理员', NULL, 'admin@manifest.local', 1, 'Seed admin user'),
  (2, 2, 'tenant_admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbDr.i', '测试租户管理员', NULL, 'tenant-admin@manifest.local', 1, 'Seed tenant admin'),
  (3, 2, 'tenant_user', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIbDr.i', '测试租户用户', NULL, 'tenant-user@manifest.local', 1, 'Seed tenant user')
ON DUPLICATE KEY UPDATE
  `password_hash` = VALUES(`password_hash`),
  `nickname` = VALUES(`nickname`),
  `email` = VALUES(`email`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`);

-- User role mappings
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
VALUES
  (1, 1),
  (2, 2),
  (3, 3)
ON DUPLICATE KEY UPDATE
  `role_id` = VALUES(`role_id`);

-- Dictionary types
INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `status`, `remark`)
VALUES
  (1, 'company_status', '公司状态', 1, 'Company status dictionary'),
  (2, 'vip_status', '会员状态', 1, 'VIP status dictionary'),
  (3, 'bl_parse_status', '提单解析状态', 1, 'Bill parse status dictionary'),
  (4, 'booking_status', '订舱单状态', 1, 'Booking order status dictionary'),
  (5, 'template_type', '模板类型', 1, 'Template type dictionary')
ON DUPLICATE KEY UPDATE
  `dict_name` = VALUES(`dict_name`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`);

-- Dictionary items
INSERT INTO `sys_dict_item`
(`dict_type_id`, `item_label`, `item_value`, `sort_no`, `status`, `remark`)
VALUES
  (1, '启用', '1', 1, 1, 'Enabled'),
  (1, '停用', '0', 2, 1, 'Disabled'),
  (2, '普通用户', '0', 1, 1, 'Normal'),
  (2, '会员用户', '1', 2, 1, 'VIP'),
  (3, '未解析', 'NONE', 1, 1, 'Not parsed'),
  (3, '处理中', 'PENDING', 2, 1, 'Pending'),
  (3, '解析成功', 'SUCCESS', 3, 1, 'Success'),
  (3, '解析失败', 'FAILED', 4, 1, 'Failed'),
  (3, '已确认', 'CONFIRMED', 5, 1, 'Confirmed'),
  (4, '草稿', 'DRAFT', 1, 1, 'Draft'),
  (4, '已提交', 'SUBMITTED', 2, 1, 'Submitted'),
  (4, '已取消', 'CANCELLED', 3, 1, 'Cancelled'),
  (5, '提单模板DOCX', 'BILL_DOCX', 1, 1, 'Bill docx template'),
  (5, '提单模板PDF', 'BILL_PDF', 2, 1, 'Bill pdf template')
ON DUPLICATE KEY UPDATE
  `item_label` = VALUES(`item_label`),
  `sort_no` = VALUES(`sort_no`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`);

