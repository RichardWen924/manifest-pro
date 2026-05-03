CREATE TABLE IF NOT EXISTS freight_demand (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    publisher_user_id BIGINT NOT NULL,
    demand_no VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    goods_name VARCHAR(255) NOT NULL,
    departure_port VARCHAR(128) NOT NULL,
    destination_port VARCHAR(128) NOT NULL,
    expected_shipping_date DATE NULL,
    quantity DECIMAL(18,2) NULL,
    quantity_unit VARCHAR(32) NULL,
    budget_amount DECIMAL(18,2) NULL,
    currency_code VARCHAR(16) NULL,
    contact_name VARCHAR(128) NULL,
    contact_phone VARCHAR(64) NULL,
    remark VARCHAR(1000) NULL,
    demand_status VARCHAR(32) NOT NULL,
    audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    audit_remark VARCHAR(1000) NULL,
    audited_by BIGINT NULL,
    audited_at DATETIME NULL,
    accepted_quote_id BIGINT NULL,
    accepted_order_id BIGINT NULL,
    hot_score BIGINT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_freight_demand_no (demand_no),
    KEY idx_freight_demand_company_status (company_id, demand_status, deleted)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS freight_demand_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    demand_id BIGINT NOT NULL,
    file_asset_id BIGINT NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_freight_demand_attachment_demand (demand_id, deleted)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS freight_quote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    demand_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    quoter_user_id BIGINT NOT NULL,
    quote_no VARCHAR(64) NOT NULL,
    price_amount DECIMAL(18,2) NOT NULL,
    currency_code VARCHAR(16) NOT NULL,
    estimated_days INT NULL,
    service_note VARCHAR(1000) NULL,
    quote_status VARCHAR(32) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_freight_quote_no (quote_no),
    KEY idx_freight_quote_demand (demand_id, quote_status, deleted)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS freight_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    demand_id BIGINT NOT NULL,
    accepted_quote_id BIGINT NOT NULL,
    publisher_company_id BIGINT NOT NULL,
    publisher_user_id BIGINT NOT NULL,
    agent_company_id BIGINT NOT NULL,
    agent_user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_freight_order_no (order_no),
    KEY idx_freight_order_demand (demand_id, order_status, deleted)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS freight_order_timeline (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_message VARCHAR(512) NOT NULL,
    operator_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_freight_order_timeline_order (order_id, created_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
