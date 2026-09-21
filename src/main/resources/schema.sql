-- V1__initial_schema.sql


create DATABASE IF NOT EXISTS fish_farm_db;
USE fish_farm_db;

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50) NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN','MANAGER','ACCOUNTANT','SALESMAN') NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    created_by    BIGINT,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by    BIGINT,
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE fish_types (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by BIGINT,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE ponds (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    pond_number VARCHAR(20) NOT NULL UNIQUE,
    pond_name   VARCHAR(100) NOT NULL,
    area_acres  DECIMAL(8,3),
    description TEXT,
    status      ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    deleted     BOOLEAN NOT NULL DEFAULT FALSE,
    created_by  BIGINT,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by  BIGINT,
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE harvests (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    harvest_number VARCHAR(20) NOT NULL UNIQUE,  -- H-20240601-001
    pond_id        BIGINT NOT NULL,
    harvest_date   DATE NOT NULL,
    remarks        TEXT,
    deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    created_by     BIGINT NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by     BIGINT,
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (pond_id) REFERENCES ponds(id)
);

CREATE TABLE harvest_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    harvest_id   BIGINT NOT NULL,
    fish_type_id BIGINT NOT NULL,
    weight_kg    DECIMAL(10,3) NOT NULL CHECK (weight_kg > 0),
    created_by   BIGINT NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by   BIGINT,
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (harvest_id)   REFERENCES harvests(id),
    FOREIGN KEY (fish_type_id) REFERENCES fish_types(id)
);

CREATE TABLE inventory (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    fish_type_id     BIGINT NOT NULL UNIQUE,
    current_stock_kg DECIMAL(12,3) NOT NULL DEFAULT 0,
    last_updated     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version          BIGINT NOT NULL DEFAULT 0,  -- optimistic locking
    FOREIGN KEY (fish_type_id) REFERENCES fish_types(id)
);

CREATE TABLE dispatches (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispatch_number VARCHAR(20) NOT NULL UNIQUE,  -- D-20240601-001
    vehicle_number  VARCHAR(20) NOT NULL,
    driver_name     VARCHAR(100) NOT NULL,
    dispatch_date   DATE NOT NULL,
    status          ENUM('CREATED','LOADING','DISPATCHED','RETURNED','CLOSED') NOT NULL DEFAULT 'CREATED',
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    created_by      BIGINT NOT NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by      BIGINT,
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

-- KEY TABLE: one row per fish type per vehicle
CREATE TABLE weighing_sessions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispatch_id  BIGINT NOT NULL,
    fish_type_id BIGINT NOT NULL,
    session_label VARCHAR(100),           -- e.g. "Baikhi - Loading 1"
    is_closed    BOOLEAN NOT NULL DEFAULT FALSE,
    created_by   BIGINT NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by   BIGINT,
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_dispatch_fish (dispatch_id, fish_type_id),
    FOREIGN KEY (dispatch_id)  REFERENCES dispatches(id),
    FOREIGN KEY (fish_type_id) REFERENCES fish_types(id)
);

-- CORE TABLE: every single basket weight — never delete, never aggregate
CREATE TABLE weight_entries (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id    BIGINT NOT NULL,
    basket_number INT NOT NULL,           -- sequence within session: 1,2,3,4...
    weight_kg     DECIMAL(8,3) NOT NULL CHECK (weight_kg > 0),
    remarks       VARCHAR(255),
    created_by    BIGINT NOT NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- No update fields: this is an immutable ledger entry
    FOREIGN KEY (session_id) REFERENCES weighing_sessions(id)
);

CREATE TABLE customers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    mobile        VARCHAR(15),
    address       TEXT,
    gst_number    VARCHAR(15),
    customer_type ENUM('RETAIL','WHOLESALE') NOT NULL DEFAULT 'RETAIL',
    deleted       BOOLEAN NOT NULL DEFAULT FALSE,
    created_by    BIGINT NOT NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by    BIGINT,
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE sales (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(20) NOT NULL UNIQUE,  -- INV-202406-0001
    customer_id    BIGINT NOT NULL,
    dispatch_id    BIGINT NOT NULL,
    sale_date      DATETIME(6) NOT NULL,
    is_inter_state BOOLEAN NOT NULL DEFAULT FALSE,
    subtotal       DECIMAL(12,2) NOT NULL,
    cgst_amount    DECIMAL(10,2) NOT NULL DEFAULT 0,
    sgst_amount    DECIMAL(10,2) NOT NULL DEFAULT 0,
    igst_amount    DECIMAL(10,2) NOT NULL DEFAULT 0,
    grand_total    DECIMAL(12,2) NOT NULL,
    deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    created_by     BIGINT NOT NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by     BIGINT,
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (dispatch_id) REFERENCES dispatches(id)
);

CREATE TABLE sale_items (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sale_id      BIGINT NOT NULL,
    fish_type_id BIGINT NOT NULL,
    quantity_kg  DECIMAL(10,3) NOT NULL CHECK (quantity_kg > 0),
    rate_per_kg  DECIMAL(8,2) NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,   -- quantity_kg * rate_per_kg
    gst_rate     DECIMAL(5,2) NOT NULL DEFAULT 5.00,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (sale_id)      REFERENCES sales(id),
    FOREIGN KEY (fish_type_id) REFERENCES fish_types(id)
);

CREATE TABLE dispatch_returns (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    dispatch_id        BIGINT NOT NULL,
    fish_type_id       BIGINT NOT NULL,
    expected_weight_kg DECIMAL(10,3) NOT NULL,  -- computed: loaded - sold
    actual_weight_kg   DECIMAL(10,3) NOT NULL,
    loss_weight_kg     DECIMAL(10,3) GENERATED ALWAYS AS
                       (expected_weight_kg - actual_weight_kg) STORED,
    remarks            TEXT,
    created_by         BIGINT NOT NULL,
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by         BIGINT,
    updated_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_dispatch_return_fish (dispatch_id, fish_type_id),
    FOREIGN KEY (dispatch_id)  REFERENCES dispatches(id),
    FOREIGN KEY (fish_type_id) REFERENCES fish_types(id)
);

CREATE TABLE expenses (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_date DATE NOT NULL,
    category     ENUM('FEED','LABOUR','DIESEL','TRANSPORT','MEDICINE','ELECTRICITY','MAINTENANCE','OTHER') NOT NULL,
    amount       DECIMAL(10,2) NOT NULL,
    remarks      TEXT,
    deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_by   BIGINT NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_by   BIGINT,
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE audit_logs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_name  VARCHAR(50) NOT NULL,
    entity_id    BIGINT NOT NULL,
    action       ENUM('CREATE','UPDATE','DELETE','STATUS_CHANGE') NOT NULL,
    old_values   JSON,
    new_values   JSON,
    performed_by BIGINT NOT NULL,
    performed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_entity (entity_name, entity_id),
    INDEX idx_audit_user   (performed_by)
);

-- Critical performance indexes
CREATE INDEX idx_weight_entries_session   ON weight_entries(session_id);
CREATE INDEX idx_weighing_sessions_dispatch ON weighing_sessions(dispatch_id);
CREATE INDEX idx_sales_dispatch           ON sales(dispatch_id);
CREATE INDEX idx_sales_customer           ON sales(customer_id);
CREATE INDEX idx_sale_items_fish_type     ON sale_items(fish_type_id);
CREATE INDEX idx_harvests_date            ON harvests(harvest_date);
CREATE INDEX idx_dispatches_date          ON dispatches(dispatch_date);
CREATE INDEX idx_dispatch_returns_dispatch ON dispatch_returns(dispatch_id);