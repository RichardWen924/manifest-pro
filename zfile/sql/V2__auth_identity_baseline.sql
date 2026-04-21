USE `manifest_refactor`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_token_session`;
DROP TABLE IF EXISTS `sys_login_log`;
DROP TABLE IF EXISTS `sys_role_permission`;
DROP TABLE IF EXISTS `sys_permission`;

CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `parent_id` bigint DEFAULT NULL COMMENT 'Parent permission id',
  `permission_code` varchar(128) NOT NULL COMMENT 'Permission code, e.g. admin:company:view',
  `permission_name` varchar(128) NOT NULL COMMENT 'Permission name',
  `resource_type` varchar(32) NOT NULL DEFAULT 'API' COMMENT 'API/MENU/BUTTON/DATA',
  `path` varchar(255) DEFAULT NULL COMMENT 'Resource path',
  `sort_no` int NOT NULL DEFAULT 1 COMMENT 'Sort number',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_permission_code` (`permission_code`),
  KEY `idx_sys_permission_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Permission resource table';

CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `role_id` bigint NOT NULL COMMENT 'Role id',
  `permission_id` bigint NOT NULL COMMENT 'Permission id',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_permission_role_permission` (`role_id`, `permission_id`),
  KEY `idx_sys_role_permission_permission_id` (`permission_id`),
  CONSTRAINT `fk_sys_role_permission_role_id`
    FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`),
  CONSTRAINT `fk_sys_role_permission_permission_id`
    FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role-permission relation';

CREATE TABLE `sys_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `user_id` bigint DEFAULT NULL COMMENT 'User id',
  `username` varchar(64) DEFAULT NULL COMMENT 'Login username',
  `login_ip` varchar(64) DEFAULT NULL COMMENT 'Login IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT 'User agent',
  `success` tinyint NOT NULL DEFAULT 0 COMMENT '1=success,0=failed',
  `message` varchar(500) DEFAULT NULL COMMENT 'Login result message',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  KEY `idx_sys_login_log_user_time` (`user_id`, `created_at`),
  KEY `idx_sys_login_log_username_time` (`username`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Login audit log';

CREATE TABLE `sys_token_session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
  `user_id` bigint NOT NULL COMMENT 'User id',
  `jti` varchar(64) NOT NULL COMMENT 'JWT id',
  `refresh_token_hash` varchar(255) NOT NULL COMMENT 'Opaque refresh token hash',
  `revoked` tinyint NOT NULL DEFAULT 0 COMMENT '1=revoked,0=active',
  `expires_at` datetime NOT NULL COMMENT 'Session expire time',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_token_session_jti` (`jti`),
  KEY `idx_sys_token_session_user_revoked` (`user_id`, `revoked`),
  KEY `idx_sys_token_session_expires_at` (`expires_at`),
  CONSTRAINT `fk_sys_token_session_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token session table';

SET FOREIGN_KEY_CHECKS = 1;
