-- =========================================================
-- Manifest Reader Refactor Sample Business Data
-- Optional seed for local development
-- =========================================================

USE `manifest_refactor`;

SET NAMES utf8mb4;

-- Sample uploaded file asset
INSERT INTO `file_asset`
(`id`, `company_id`, `biz_type`, `file_name`, `original_name`, `content_type`, `file_size`, `storage_type`, `bucket_name`, `object_key`, `file_hash`, `status`, `created_by`)
VALUES
  (1001, 2, 'UPLOAD', 'sample-bl.docx', 'sample-bl.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 102400, 'MINIO', 'manifest', 'uploads/sample-bl.docx', NULL, 1, 2)
ON DUPLICATE KEY UPDATE
  `original_name` = VALUES(`original_name`),
  `content_type` = VALUES(`content_type`),
  `file_size` = VALUES(`file_size`),
  `storage_type` = VALUES(`storage_type`),
  `bucket_name` = VALUES(`bucket_name`),
  `object_key` = VALUES(`object_key`),
  `status` = VALUES(`status`);

-- Sample bill document
INSERT INTO `bl_document`
(`id`, `company_id`, `bl_no`, `booking_no`, `doc_no`, `serial_no`, `vessel_voyage`, `pre_carriage_by`, `place_of_receipt`, `port_of_loading`, `port_of_discharge`, `place_of_delivery`, `source_file_id`, `status`, `parse_status`, `remark`, `created_by`, `updated_by`)
VALUES
  (2001, 2, 'TEST-BL-0001', 'BOOK-0001', 'TEST20260001', 'SER-0001', 'MSC TEST V001', 'TRUCK', 'SHANGHAI', 'SHANGHAI', 'SINGAPORE', 'SINGAPORE', 1001, 'CONFIRMED', 'CONFIRMED', 'Sample bill document', 2, 2)
ON DUPLICATE KEY UPDATE
  `booking_no` = VALUES(`booking_no`),
  `doc_no` = VALUES(`doc_no`),
  `status` = VALUES(`status`),
  `parse_status` = VALUES(`parse_status`),
  `updated_by` = VALUES(`updated_by`);

INSERT INTO `bl_party`
(`bl_document_id`, `party_type`, `party_name`, `party_address`, `contact_name`, `contact_phone`, `contact_email`, `sort_no`)
VALUES
  (2001, 'SHIPPER', 'TEST SHIPPER LTD.', 'Shanghai, China', 'Alice', '13800000001', 'alice@test.com', 1),
  (2001, 'CONSIGNEE', 'TEST CONSIGNEE PTE.', 'Singapore', 'Bob', '13800000002', 'bob@test.com', 2),
  (2001, 'NOTIFY', 'TEST NOTIFY CO.', 'Singapore', 'Carol', '13800000003', 'carol@test.com', 3);

INSERT INTO `bl_cargo_item`
(`bl_document_id`, `container_no`, `seal_no`, `container_type`, `package_quantity`, `package_unit`, `goods_description`, `marks`, `container_weight`, `vgm_weight`, `gross_weight_kgs`, `measurement_cbm`, `sort_no`)
VALUES
  (2001, 'TGHU1234567', 'SEAL123', '40HQ', 100, 'CTN', 'Electronic parts', 'TEST MARKS', 2100.000, 23500.000, 23000.000, 68.500, 1);

INSERT INTO `bl_charge`
(`bl_document_id`, `charge_type`, `charge_name`, `currency_code`, `amount`, `payment_term`, `payable_at`, `sort_no`)
VALUES
  (2001, 'FREIGHT', 'Ocean Freight', 'USD', 1200.00, 'PREPAID', 'SHANGHAI', 1),
  (2001, 'OTHER', 'Documentation Fee', 'USD', 50.00, 'PREPAID', 'SHANGHAI', 2);

INSERT INTO `bl_issue_info`
(`bl_document_id`, `service_type`, `service_mode`, `revenue_tons`, `issue_place`, `laden_on_board`, `original_bl_count`)
VALUES
  (2001, 'CY-CY', 'FCL', '23RT', 'SHANGHAI', '2026-04-20', '3')
ON DUPLICATE KEY UPDATE
  `service_type` = VALUES(`service_type`),
  `service_mode` = VALUES(`service_mode`),
  `revenue_tons` = VALUES(`revenue_tons`),
  `issue_place` = VALUES(`issue_place`),
  `laden_on_board` = VALUES(`laden_on_board`),
  `original_bl_count` = VALUES(`original_bl_count`);

INSERT INTO `bl_parse_task`
(`id`, `task_no`, `company_id`, `source_file_id`, `task_status`, `engine_type`, `request_payload`, `result_payload`, `started_at`, `finished_at`)
VALUES
  (3001, 'PARSE-20260421-0001', 2, 1001, 'CONFIRMED', 'MANUAL',
   JSON_OBJECT('fileAssetId', 1001),
   JSON_OBJECT('billId', 2001, 'status', 'confirmed'),
   NOW(), NOW())
ON DUPLICATE KEY UPDATE
  `task_status` = VALUES(`task_status`),
  `result_payload` = VALUES(`result_payload`),
  `finished_at` = VALUES(`finished_at`);

-- Sample template
INSERT INTO `file_asset`
(`id`, `company_id`, `biz_type`, `file_name`, `original_name`, `content_type`, `file_size`, `storage_type`, `bucket_name`, `object_key`, `status`, `created_by`)
VALUES
  (1002, 2, 'TEMPLATE', 'bill-template-v1.docx', 'bill-template-v1.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 204800, 'MINIO', 'manifest', 'templates/bill-template-v1.docx', 1, 2)
ON DUPLICATE KEY UPDATE
  `object_key` = VALUES(`object_key`),
  `status` = VALUES(`status`);

INSERT INTO `tpl_template`
(`id`, `company_id`, `template_code`, `template_name`, `template_type`, `status`, `remark`, `created_by`, `updated_by`)
VALUES
  (4001, 2, 'BILL_DEFAULT', '默认提单模板', 'BILL_DOCX', 1, 'Default bill template', 2, 2)
ON DUPLICATE KEY UPDATE
  `template_name` = VALUES(`template_name`),
  `status` = VALUES(`status`),
  `updated_by` = VALUES(`updated_by`);

INSERT INTO `tpl_template_version`
(`id`, `template_id`, `version_no`, `file_asset_id`, `content_format`, `field_schema_json`, `status`, `created_by`)
VALUES
  (4101, 4001, 1, 1002, 'DOCX',
   JSON_OBJECT('fields', JSON_ARRAY('blNo', 'bookingNo', 'shipper', 'consignee')),
   1, 2)
ON DUPLICATE KEY UPDATE
  `file_asset_id` = VALUES(`file_asset_id`),
  `field_schema_json` = VALUES(`field_schema_json`),
  `status` = VALUES(`status`);

UPDATE `tpl_template`
SET `current_version_id` = 4101
WHERE `id` = 4001;

INSERT INTO `tpl_field_mapping`
(`template_version_id`, `field_key`, `field_name`, `source_text`, `data_type`, `required_flag`, `default_value`, `sort_no`)
VALUES
  (4101, 'blNo', '提单号', 'B/L NO.', 'STRING', 1, NULL, 1),
  (4101, 'bookingNo', '订舱号', 'BOOKING NO.', 'STRING', 0, NULL, 2),
  (4101, 'shipper', '发货人', 'SHIPPER', 'STRING', 1, NULL, 3),
  (4101, 'consignee', '收货人', 'CONSIGNEE', 'STRING', 1, NULL, 4);

-- Sample booking order
INSERT INTO `booking_order`
(`id`, `company_id`, `bl_no`, `booking_no`, `doc_no`, `vessel_name`, `voyage_no`, `port_of_loading`, `port_of_discharge`, `goods_description`, `gross_weight_kgs`, `measurement_cbm`, `status`, `remark`, `created_by`, `updated_by`)
VALUES
  (5001, 2, 'TEST-BL-0001', 'BOOK-0001', 'TEST20260001', 'MSC TEST', 'V001', 'SHANGHAI', 'SINGAPORE', 'Electronic parts', 23000.000, 68.500, 'SUBMITTED', 'Sample booking order', 2, 2)
ON DUPLICATE KEY UPDATE
  `status` = VALUES(`status`),
  `updated_by` = VALUES(`updated_by`);

-- Sample operation log
INSERT INTO `sys_oper_log`
(`id`, `company_id`, `user_id`, `module_code`, `biz_type`, `biz_id`, `request_uri`, `request_method`, `request_ip`, `oper_status`, `error_message`, `cost_ms`)
VALUES
  (6001, 2, 2, 'BILL', 'CREATE', '2001', '/user/bill/confirm', 'POST', '127.0.0.1', 1, NULL, 128)
ON DUPLICATE KEY UPDATE
  `oper_status` = VALUES(`oper_status`),
  `cost_ms` = VALUES(`cost_ms`);

