USE `manifest_refactor`;

SET NAMES utf8mb4;

ALTER TABLE `bl_parse_task`
  ADD COLUMN `task_type` varchar(32) NOT NULL DEFAULT 'BILL_EXTRACT' COMMENT 'BILL_EXTRACT/TEMPLATE_EXPORT/TEMPLATE_EXTRACT' AFTER `source_file_id`,
  ADD COLUMN `biz_id` bigint DEFAULT NULL COMMENT 'Business target id, such as template id' AFTER `engine_type`,
  ADD COLUMN `output_format` varchar(16) DEFAULT NULL COMMENT 'DOCX/PDF for export tasks' AFTER `file_hash`,
  ADD COLUMN `result_file_id` bigint DEFAULT NULL COMMENT 'Generated file asset id' AFTER `result_payload`;

ALTER TABLE `bl_parse_task`
  ADD KEY `idx_bl_parse_task_company_type_status` (`company_id`, `task_type`, `task_status`),
  ADD KEY `idx_bl_parse_task_result_file` (`result_file_id`);
