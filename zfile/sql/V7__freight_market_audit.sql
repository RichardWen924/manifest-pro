SET @audit_status_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'freight_demand'
      AND COLUMN_NAME = 'audit_status'
);
SET @audit_status_sql = IF(
    @audit_status_exists = 0,
    'ALTER TABLE freight_demand ADD COLUMN audit_status VARCHAR(32) NOT NULL DEFAULT ''PENDING'' AFTER demand_status',
    'SELECT 1'
);
PREPARE audit_status_stmt FROM @audit_status_sql;
EXECUTE audit_status_stmt;
DEALLOCATE PREPARE audit_status_stmt;

SET @audit_remark_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'freight_demand'
      AND COLUMN_NAME = 'audit_remark'
);
SET @audit_remark_sql = IF(
    @audit_remark_exists = 0,
    'ALTER TABLE freight_demand ADD COLUMN audit_remark VARCHAR(512) NULL AFTER audit_status',
    'SELECT 1'
);
PREPARE audit_remark_stmt FROM @audit_remark_sql;
EXECUTE audit_remark_stmt;
DEALLOCATE PREPARE audit_remark_stmt;

SET @audited_by_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'freight_demand'
      AND COLUMN_NAME = 'audited_by'
);
SET @audited_by_sql = IF(
    @audited_by_exists = 0,
    'ALTER TABLE freight_demand ADD COLUMN audited_by BIGINT NULL AFTER audit_remark',
    'SELECT 1'
);
PREPARE audited_by_stmt FROM @audited_by_sql;
EXECUTE audited_by_stmt;
DEALLOCATE PREPARE audited_by_stmt;

SET @audited_at_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'freight_demand'
      AND COLUMN_NAME = 'audited_at'
);
SET @audited_at_sql = IF(
    @audited_at_exists = 0,
    'ALTER TABLE freight_demand ADD COLUMN audited_at DATETIME NULL AFTER audited_by',
    'SELECT 1'
);
PREPARE audited_at_stmt FROM @audited_at_sql;
EXECUTE audited_at_stmt;
DEALLOCATE PREPARE audited_at_stmt;
