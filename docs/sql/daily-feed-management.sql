-- Run manually in the application's existing MySQL 8 database after taking a backup.
-- Select your database first (e.g. USE fish_farm_db;).
-- Additive migration: existing pond, harvest, inventory and dispatch data are unchanged.
CREATE TABLE IF NOT EXISTS feed_types (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    size_mm INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    rate_per_kg DECIMAL(10,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_feed_size UNIQUE (size_mm),
    CONSTRAINT ck_feed_size CHECK (size_mm BETWEEN 1 AND 4),
    CONSTRAINT ck_feed_rate CHECK (rate_per_kg > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS daily_feeds (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pond_id BIGINT NOT NULL,
    feed_type_id BIGINT NOT NULL,
    feeding_date DATE NOT NULL,
    feeding_time VARCHAR(10) NOT NULL,
    quantity_kg DECIMAL(10,3) NOT NULL,
    rate_per_kg DECIMAL(10,2) NOT NULL,
    expense DECIMAL(18,2) NOT NULL,
    notes VARCHAR(500),
    CONSTRAINT uq_daily_feed UNIQUE (pond_id, feeding_date, feeding_time, feed_type_id),
    CONSTRAINT fk_daily_feed_pond FOREIGN KEY (pond_id) REFERENCES ponds(id),
    CONSTRAINT fk_daily_feed_type FOREIGN KEY (feed_type_id) REFERENCES feed_types(id),
    CONSTRAINT ck_feeding_time CHECK (feeding_time IN ('MORNING', 'EVENING')),
    CONSTRAINT ck_feed_quantity CHECK (quantity_kg > 0),
    CONSTRAINT ck_daily_feed_rate CHECK (rate_per_kg > 0),
    CONSTRAINT ck_feed_expense CHECK (expense = ROUND(quantity_kg * rate_per_kg, 2)),
    INDEX idx_daily_feed_date (feeding_date)
) ENGINE=InnoDB;

-- No invented prices are inserted. Add the actual 1, 2, 3 and 4 mm feed prices
-- through Daily Feed > Feed Master, or use INSERT with your actual values.
-- Expense report example (adjust dates and optionally add AND f.pond_id = ...):
-- SELECT f.feeding_date, p.pond_name, SUM(f.quantity_kg) AS total_kg,
--        SUM(f.expense) AS feed_expense
-- FROM daily_feeds f JOIN ponds p ON p.id = f.pond_id
-- WHERE f.feeding_date BETWEEN '2026-11-01' AND '2026-11-30'
-- GROUP BY f.feeding_date, p.id, p.pond_name;


-- One persisted feeding setup per pond. Existing legacy tables are retained.
CREATE TABLE IF NOT EXISTS pond_feed_configs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pond_id BIGINT NOT NULL,
    feed_type_id BIGINT NOT NULL,
    morning_qty_kg DECIMAL(10,3) NOT NULL,
    evening_qty_kg DECIMAL(10,3) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_pond_feed_config UNIQUE (pond_id),
    CONSTRAINT fk_pond_feed_config_pond FOREIGN KEY (pond_id) REFERENCES ponds(id),
    CONSTRAINT fk_pond_feed_config_type FOREIGN KEY (feed_type_id) REFERENCES feed_types(id),
    CONSTRAINT ck_pond_morning_qty CHECK (morning_qty_kg >= 0),
    CONSTRAINT ck_pond_evening_qty CHECK (evening_qty_kg >= 0)
) ENGINE=InnoDB;
