-- =========================================================
-- Manifest Reader Refactor Schema
-- Target: MySQL 8.0+
-- Purpose:
-- 1. Split legacy mixed tables into normalized domain tables
-- 2. Support auth/admin/user microservice boundaries
-- 3. Keep file/template/parse-task as independent capabilities
-- =========================================================

CREATE DATABASE IF NOT EXISTS `manifest_refactor`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `manifest_refactor`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- Auth / Tenant Domain
-- =========================

DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;
DROP TABLE IF EXISTS `sys_company`;

CREATE TABLE `sys_company` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `company_code` varchar(32) NOT NULL COMMENT 'Tenant code',
  `company_name` varchar(128) NOT NULL COMMENT 'Tenant/company name',
  `company_abbr` varchar(16) NOT NULL COMMENT 'Company abbreviation',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  `vip_status` tinyint NOT NULL DEFAULT 0 COMMENT '1=vip,0=normal',
  `package_type` varchar(32) DEFAULT NULL COMMENT 'Plan/package type',
  `expire_at` datetime DEFAULT NULL COMMENT 'Subscription expire time',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0=normal,1=deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_company_code` (`company_code`),
  UNIQUE KEY `uk_sys_company_abbr` (`company_abbr`),
  KEY `idx_sys_company_status_expire` (`status`, `expire_at`),
  KEY `idx_sys_company_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Company / tenant master table';

CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `company_id` bigint NOT NULL COMMENT 'Tenant id',
  `username` varchar(64) NOT NULL COMMENT 'Login username',
  `password_hash` varchar(255) NOT NULL COMMENT 'BCrypt password hash',
  `nickname` varchar(64) DEFAULT NULL COMMENT 'Display name',
  `mobile` varchar(32) DEFAULT NULL COMMENT 'Mobile phone',
  `email` varchar(128) DEFAULT NULL COMMENT 'Email',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  `last_login_at` datetime DEFAULT NULL COMMENT 'Last login time',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0=normal,1=deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_company_username` (`company_id`, `username`),
  KEY `idx_sys_user_company_status` (`company_id`, `status`),
  KEY `idx_sys_user_mobile` (`mobile`),
  KEY `idx_sys_user_email` (`email`),
  KEY `idx_sys_user_deleted` (`deleted`),
  CONSTRAINT `fk_sys_user_company_id`
    FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User account table';

CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `role_code` varchar(64) NOT NULL COMMENT 'Role code',
  `role_name` varchar(128) NOT NULL COMMENT 'Role name',
  `role_scope` varchar(32) NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/ADMIN/USER',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role table';

CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `user_id` bigint NOT NULL COMMENT 'User id',
  `role_id` bigint NOT NULL COMMENT 'Role id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role_user_role` (`user_id`, `role_id`),
  KEY `idx_sys_user_role_role_id` (`role_id`),
  CONSTRAINT `fk_sys_user_role_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_sys_user_role_role_id`
    FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-role relation';

-- =========================
-- File Domain
-- =========================

DROP TABLE IF EXISTS `file_asset`;

CREATE TABLE `file_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `company_id` bigint DEFAULT NULL COMMENT 'Tenant id, null for system files',
  `biz_type` varchar(32) NOT NULL COMMENT 'UPLOAD/TEMPLATE/EXPORT/IMPORT',
  `file_name` varchar(255) NOT NULL COMMENT 'Stored file name',
  `original_name` varchar(255) NOT NULL COMMENT 'Original uploaded name',
  `content_type` varchar(128) DEFAULT NULL COMMENT 'Mime type',
  `file_size` bigint NOT NULL DEFAULT 0 COMMENT 'Byte size',
  `storage_type` varchar(32) NOT NULL DEFAULT 'MINIO' COMMENT 'MINIO/LOCAL',
  `bucket_name` varchar(64) DEFAULT NULL COMMENT 'Bucket',
  `object_key` varchar(255) NOT NULL COMMENT 'Object key/path',
  `file_hash` varchar(64) DEFAULT NULL COMMENT 'SHA-256/MD5 hash',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=available,0=disabled',
  `created_by` bigint DEFAULT NULL COMMENT 'Creator user id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0=normal,1=deleted',
  PRIMARY KEY (`id`),
  KEY `idx_file_asset_company_biz_type` (`company_id`, `biz_type`),
  KEY `idx_file_asset_object_key` (`object_key`),
  KEY `idx_file_asset_file_hash` (`file_hash`),
  KEY `idx_file_asset_deleted` (`deleted`),
  CONSTRAINT `fk_file_asset_company_id`
    FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Unified file asset table';

-- =========================
-- Bill of Lading Domain
-- =========================

DROP TABLE IF EXISTS `bl_charge`;
DROP TABLE IF EXISTS `bl_cargo_item`;
DROP TABLE IF EXISTS `bl_party`;
DROP TABLE IF EXISTS `bl_issue_info`;
DROP TABLE IF EXISTS `bl_parse_task`;
DROP TABLE IF EXISTS `bl_document`;

CREATE TABLE `bl_document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `company_id` bigint NOT NULL COMMENT 'Tenant id',
  `bl_no` varchar(64) NOT NULL COMMENT 'Bill of lading no',
  `booking_no` varchar(64) DEFAULT NULL COMMENT 'Booking no',
  `doc_no` varchar(64) DEFAULT NULL COMMENT 'Document no',
  `serial_no` varchar(64) DEFAULT NULL COMMENT 'Serial no',
  `vessel_voyage` varchar(128) DEFAULT NULL COMMENT 'Vessel/voyage',
  `pre_carriage_by` varchar(128) DEFAULT NULL COMMENT 'Pre-carriage by',
  `place_of_receipt` varchar(128) DEFAULT NULL COMMENT 'Place of receipt',
  `port_of_loading` varchar(128) DEFAULT NULL COMMENT 'Port of loading',
  `port_of_discharge` varchar(128) DEFAULT NULL COMMENT 'Port of discharge',
  `place_of_delivery` varchar(128) DEFAULT NULL COMMENT 'Place of delivery',
  `source_file_id` bigint DEFAULT NULL COMMENT 'Source file asset id',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/ARCHIVED',
  `parse_status` varchar(32) NOT NULL DEFAULT 'NONE' COMMENT 'NONE/PENDING/SUCCESS/FAILED/CONFIRMED',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `created_by` bigint DEFAULT NULL COMMENT 'Creator user id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_by` bigint DEFAULT NULL COMMENT 'Updater user id',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0=normal,1=deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bl_document_company_bl_no` (`company_id`, `bl_no`),
  KEY `idx_bl_document_company_created` (`company_id`, `created_at`),
  KEY `idx_bl_document_company_booking_no` (`company_id`, `booking_no`),
  KEY `idx_bl_document_company_doc_no` (`company_id`, `doc_no`),
  KEY `idx_bl_document_parse_status` (`company_id`, `parse_status`),
  KEY `idx_bl_document_deleted` (`deleted`),
  CONSTRAINT `fk_bl_document_company_id`
    FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`id`),
  CONSTRAINT `fk_bl_document_source_file_id`
    FOREIGN KEY (`source_file_id`) REFERENCES `file_asset` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bill of lading master table';

CREATE TABLE `bl_party` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `bl_document_id` bigint NOT NULL COMMENT 'Bill document id',
  `party_type` varchar(32) NOT NULL COMMENT 'SHIPPER/CONSIGNEE/NOTIFY/CARRIER_AGENT/DELIVERY_AGENT',
  `party_name` varchar(255) NOT NULL COMMENT 'Party name',
  `party_address` varchar(500) DEFAULT NULL COMMENT 'Party address',
  `contact_name` varchar(128) DEFAULT NULL COMMENT 'Contact name',
  `contact_phone` varchar(64) DEFAULT NULL COMMENT 'Contact phone',
  `contact_email` varchar(128) DEFAULT NULL COMMENT 'Contact email',
  `sort_no` int NOT NULL DEFAULT 1 COMMENT 'Sort number',
  PRIMARY KEY (`id`),
  KEY `idx_bl_party_document_type` (`bl_document_id`, `party_type`),
  CONSTRAINT `fk_bl_party_document_id`
    FOREIGN KEY (`bl_document_id`) REFERENCES `bl_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bill party table';

CREATE TABLE `bl_cargo_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `bl_document_id` bigint NOT NULL COMMENT 'Bill document id',
  `container_no` varchar(64) DEFAULT NULL COMMENT 'Container no',
  `seal_no` varchar(64) DEFAULT NULL COMMENT 'Seal no',
  `container_type` varchar(32) DEFAULT NULL COMMENT 'Container type',
  `package_quantity` int DEFAULT NULL COMMENT 'Package quantity',
  `package_unit` varchar(32) DEFAULT NULL COMMENT 'Package unit',
  `goods_description` varchar(1000) DEFAULT NULL COMMENT 'Goods description',
  `marks` varchar(500) DEFAULT NULL COMMENT 'Marks',
  `container_weight` decimal(18,3) DEFAULT NULL COMMENT 'Container weight',
  `vgm_weight` decimal(18,3) DEFAULT NULL COMMENT 'VGM weight',
  `gross_weight_kgs` decimal(18,3) DEFAULT NULL COMMENT 'Gross weight',
  `measurement_cbm` decimal(18,3) DEFAULT NULL COMMENT 'Measurement',
  `sort_no` int NOT NULL DEFAULT 1 COMMENT 'Sort number',
  PRIMARY KEY (`id`),
  KEY `idx_bl_cargo_item_document_sort` (`bl_document_id`, `sort_no`),
  CONSTRAINT `fk_bl_cargo_item_document_id`
    FOREIGN KEY (`bl_document_id`) REFERENCES `bl_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bill cargo item table';

CREATE TABLE `bl_charge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `bl_document_id` bigint NOT NULL COMMENT 'Bill document id',
  `charge_type` varchar(64) NOT NULL COMMENT 'FREIGHT/PREPAID/COLLECT/OTHER',
  `charge_name` varchar(128) NOT NULL COMMENT 'Charge name',
  `currency_code` varchar(16) DEFAULT NULL COMMENT 'Currency code',
  `amount` decimal(18,2) DEFAULT NULL COMMENT 'Amount',
  `payment_term` varchar(32) DEFAULT NULL COMMENT 'PREPAID/COLLECT',
  `payable_at` varchar(128) DEFAULT NULL COMMENT 'Payable at',
  `sort_no` int NOT NULL DEFAULT 1 COMMENT 'Sort number',
  PRIMARY KEY (`id`),
  KEY `idx_bl_charge_document_sort` (`bl_document_id`, `sort_no`),
  CONSTRAINT `fk_bl_charge_document_id`
    FOREIGN KEY (`bl_document_id`) REFERENCES `bl_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bill charge table';

CREATE TABLE `bl_issue_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `bl_document_id` bigint NOT NULL COMMENT 'Bill document id',
  `service_type` varchar(64) DEFAULT NULL COMMENT 'Service type',
  `service_mode` varchar(64) DEFAULT NULL COMMENT 'Service mode',
  `revenue_tons` varchar(64) DEFAULT NULL COMMENT 'Revenue tons',
  `issue_place` varchar(128) DEFAULT NULL COMMENT 'Issue place',
  `laden_on_board` varchar(128) DEFAULT NULL COMMENT 'Laden on board',
  `original_bl_count` varchar(64) DEFAULT NULL COMMENT 'Original bill count',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bl_issue_info_document_id` (`bl_document_id`),
  CONSTRAINT `fk_bl_issue_info_document_id`
    FOREIGN KEY (`bl_document_id`) REFERENCES `bl_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bill issue info table';

CREATE TABLE `bl_parse_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `task_no` varchar(64) NOT NULL COMMENT 'Unique task no',
  `company_id` bigint NOT NULL COMMENT 'Tenant id',
  `source_file_id` bigint NOT NULL COMMENT 'Source file id',
  `task_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED/CONFIRMED',
  `engine_type` varchar(32) DEFAULT NULL COMMENT 'DIFY/LOCAL/MANUAL',
  `request_payload` json DEFAULT NULL COMMENT 'Request snapshot',
  `result_payload` json DEFAULT NULL COMMENT 'Result snapshot',
  `error_message` varchar(1000) DEFAULT NULL COMMENT 'Error message',
  `started_at` datetime DEFAULT NULL COMMENT 'Start time',
  `finished_at` datetime DEFAULT NULL COMMENT 'Finish time',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bl_parse_task_task_no` (`task_no`),
  KEY `idx_bl_parse_task_company_status` (`company_id`, `task_status`),
  KEY `idx_bl_parse_task_source_file` (`source_file_id`),
  CONSTRAINT `fk_bl_parse_task_company_id`
    FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`id`),
  CONSTRAINT `fk_bl_parse_task_source_file_id`
    FOREIGN KEY (`source_file_id`) REFERENCES `file_asset` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bill parse task table';

-- =========================
-- Template Domain
-- =========================

DROP TABLE IF EXISTS `tpl_field_mapping`;
DROP TABLE IF EXISTS `tpl_template_version`;
DROP TABLE IF EXISTS `tpl_template`;

CREATE TABLE `tpl_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `company_id` bigint DEFAULT NULL COMMENT 'Tenant id, null for platform templates',
  `template_code` varchar(64) NOT NULL COMMENT 'Template code',
  `template_name` varchar(128) NOT NULL COMMENT 'Template name',
  `template_type` varchar(32) NOT NULL DEFAULT 'BILL_DOCX' COMMENT 'BILL_DOCX/BILL_PDF/OTHER',
  `current_version_id` bigint DEFAULT NULL COMMENT 'Current version id',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `created_by` bigint DEFAULT NULL COMMENT 'Creator user id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_by` bigint DEFAULT NULL COMMENT 'Updater user id',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0=normal,1=deleted',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tpl_template_company_code` (`company_id`, `template_code`),
  KEY `idx_tpl_template_company_type` (`company_id`, `template_type`),
  KEY `idx_tpl_template_deleted` (`deleted`),
  CONSTRAINT `fk_tpl_template_company_id`
    FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Template master table';

CREATE TABLE `tpl_template_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `template_id` bigint NOT NULL COMMENT 'Template id',
  `version_no` int NOT NULL COMMENT 'Version no',
  `file_asset_id` bigint NOT NULL COMMENT 'Template source file',
  `content_format` varchar(32) NOT NULL DEFAULT 'DOCX' COMMENT 'DOCX/HTML/PDF',
  `field_schema_json` json DEFAULT NULL COMMENT 'Field schema snapshot',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  `created_by` bigint DEFAULT NULL COMMENT 'Creator user id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tpl_template_version` (`template_id`, `version_no`),
  KEY `idx_tpl_template_version_file_asset` (`file_asset_id`),
  CONSTRAINT `fk_tpl_template_version_template_id`
    FOREIGN KEY (`template_id`) REFERENCES `tpl_template` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_tpl_template_version_file_asset_id`
    FOREIGN KEY (`file_asset_id`) REFERENCES `file_asset` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Template version table';

CREATE TABLE `tpl_field_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `template_version_id` bigint NOT NULL COMMENT 'Template version id',
  `field_key` varchar(64) NOT NULL COMMENT 'System field key',
  `field_name` varchar(128) NOT NULL COMMENT 'Field display name',
  `source_text` varchar(255) DEFAULT NULL COMMENT 'Original source text',
  `data_type` varchar(32) NOT NULL DEFAULT 'STRING' COMMENT 'STRING/NUMBER/DATE/DECIMAL',
  `required_flag` tinyint NOT NULL DEFAULT 0 COMMENT '1=required,0=optional',
  `default_value` varchar(255) DEFAULT NULL COMMENT 'Default value',
  `sort_no` int NOT NULL DEFAULT 1 COMMENT 'Sort number',
  PRIMARY KEY (`id`),
  KEY `idx_tpl_field_mapping_version_sort` (`template_version_id`, `sort_no`),
  CONSTRAINT `fk_tpl_field_mapping_version_id`
    FOREIGN KEY (`template_version_id`) REFERENCES `tpl_template_version` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Template field mapping table';

-- =========================
-- Booking Domain
-- =========================

DROP TABLE IF EXISTS `booking_order`;

CREATE TABLE `booking_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `company_id` bigint NOT NULL COMMENT 'Tenant id',
  `bl_no` varchar(64) DEFAULT NULL COMMENT 'Bill no',
  `booking_no` varchar(64) DEFAULT NULL COMMENT 'Booking no',
  `doc_no` varchar(64) DEFAULT NULL COMMENT 'Document no',
  `vessel_name` varchar(128) DEFAULT NULL COMMENT 'Vessel name',
  `voyage_no` varchar(64) DEFAULT NULL COMMENT 'Voyage no',
  `port_of_loading` varchar(128) DEFAULT NULL COMMENT 'Port of loading',
  `port_of_discharge` varchar(128) DEFAULT NULL COMMENT 'Port of discharge',
  `goods_description` varchar(1000) DEFAULT NULL COMMENT 'Goods description',
  `gross_weight_kgs` decimal(18,3) DEFAULT NULL COMMENT 'Gross weight',
  `measurement_cbm` decimal(18,3) DEFAULT NULL COMMENT 'Measurement',
  `status` varchar(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/CANCELLED',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `created_by` bigint DEFAULT NULL COMMENT 'Creator user id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_by` bigint DEFAULT NULL COMMENT 'Updater user id',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0=normal,1=deleted',
  PRIMARY KEY (`id`),
  KEY `idx_booking_order_company_created` (`company_id`, `created_at`),
  KEY `idx_booking_order_company_booking_no` (`company_id`, `booking_no`),
  KEY `idx_booking_order_deleted` (`deleted`),
  CONSTRAINT `fk_booking_order_company_id`
    FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Booking consolidated order table';

-- =========================
-- Dict / Log Domain
-- =========================

DROP TABLE IF EXISTS `sys_oper_log`;
DROP TABLE IF EXISTS `sys_dict_item`;
DROP TABLE IF EXISTS `sys_dict_type`;

CREATE TABLE `sys_dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `dict_type` varchar(64) NOT NULL COMMENT 'Type code',
  `dict_name` varchar(128) NOT NULL COMMENT 'Type name',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dictionary type table';

CREATE TABLE `sys_dict_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `dict_type_id` bigint NOT NULL COMMENT 'Dictionary type id',
  `item_label` varchar(128) NOT NULL COMMENT 'Display label',
  `item_value` varchar(128) NOT NULL COMMENT 'Stored value',
  `sort_no` int NOT NULL DEFAULT 1 COMMENT 'Sort number',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_dict_item_type_value` (`dict_type_id`, `item_value`),
  KEY `idx_sys_dict_item_type_sort` (`dict_type_id`, `sort_no`),
  CONSTRAINT `fk_sys_dict_item_type_id`
    FOREIGN KEY (`dict_type_id`) REFERENCES `sys_dict_type` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dictionary item table';

CREATE TABLE `sys_oper_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `company_id` bigint DEFAULT NULL COMMENT 'Tenant id',
  `user_id` bigint DEFAULT NULL COMMENT 'Operator user id',
  `module_code` varchar(64) NOT NULL COMMENT 'Module code',
  `biz_type` varchar(64) DEFAULT NULL COMMENT 'Business action type',
  `biz_id` varchar(64) DEFAULT NULL COMMENT 'Business id',
  `request_uri` varchar(255) DEFAULT NULL COMMENT 'Request URI',
  `request_method` varchar(16) DEFAULT NULL COMMENT 'GET/POST/PUT/DELETE',
  `request_ip` varchar(64) DEFAULT NULL COMMENT 'Client IP',
  `oper_status` tinyint NOT NULL DEFAULT 1 COMMENT '1=success,0=failed',
  `error_message` varchar(1000) DEFAULT NULL COMMENT 'Error message',
  `cost_ms` bigint DEFAULT NULL COMMENT 'Cost milliseconds',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  KEY `idx_sys_oper_log_company_created` (`company_id`, `created_at`),
  KEY `idx_sys_oper_log_user_created` (`user_id`, `created_at`),
  KEY `idx_sys_oper_log_module_created` (`module_code`, `created_at`),
  CONSTRAINT `fk_sys_oper_log_company_id`
    FOREIGN KEY (`company_id`) REFERENCES `sys_company` (`id`),
  CONSTRAINT `fk_sys_oper_log_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Operation log table';

SET FOREIGN_KEY_CHECKS = 1;

