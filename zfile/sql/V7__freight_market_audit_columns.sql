ALTER TABLE freight_demand
    ADD COLUMN IF NOT EXISTS audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' AFTER demand_status,
    ADD COLUMN IF NOT EXISTS audit_remark VARCHAR(1000) NULL AFTER audit_status,
    ADD COLUMN IF NOT EXISTS audited_by BIGINT NULL AFTER audit_remark,
    ADD COLUMN IF NOT EXISTS audited_at DATETIME NULL AFTER audited_by;

UPDATE freight_demand
SET audit_status = CASE
    WHEN demand_status = 'PENDING_REVIEW' THEN 'PENDING'
    WHEN demand_status = 'REJECTED' THEN 'REJECTED'
    ELSE 'APPROVED'
END
WHERE audit_status IS NULL
   OR audit_status = '';
