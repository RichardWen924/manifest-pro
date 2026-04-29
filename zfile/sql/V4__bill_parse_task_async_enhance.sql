USE `manifest_refactor`;

SET NAMES utf8mb4;

ALTER TABLE `bl_parse_task`
  MODIFY COLUMN `source_file_id` bigint NULL COMMENT 'Source file id, nullable for async temp uploads',
  ADD COLUMN `file_name` varchar(255) DEFAULT NULL COMMENT 'Original upload file name' AFTER `engine_type`,
  ADD COLUMN `file_hash` varchar(64) DEFAULT NULL COMMENT 'SHA-256 file hash for idempotency' AFTER `file_name`,
  ADD COLUMN `created_by` bigint DEFAULT NULL COMMENT 'Creator user id' AFTER `finished_at`,
  ADD COLUMN `updated_by` bigint DEFAULT NULL COMMENT 'Updater user id' AFTER `created_by`;

ALTER TABLE `bl_parse_task`
  ADD KEY `idx_bl_parse_task_company_hash` (`company_id`, `file_hash`);
