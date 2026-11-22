-- Select the existing application database. Additive: no existing fish tables change.
CREATE TABLE IF NOT EXISTS medicine_products (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL, brand VARCHAR(60) NOT NULL,
 pack_label VARCHAR(40) NOT NULL, unit VARCHAR(8) NOT NULL,
 minimum_stock INT NOT NULL DEFAULT 0,
 selling_price DECIMAL(12,2) NOT NULL DEFAULT 0,
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 farm_id BIGINT NOT NULL,
 recorded_by BIGINT NULL, recorded_at DATETIME(6) NULL,
 last_changed_by BIGINT NULL, last_changed_at DATETIME(6) NULL,
 UNIQUE KEY uq_medicine_product (farm_id,name,brand,pack_label),
 CONSTRAINT fk_med_product_farm FOREIGN KEY (farm_id) REFERENCES farms(id),
 CONSTRAINT ck_med_product CHECK (unit IN ('BOTTLE','PACKET') AND minimum_stock >= 0 AND selling_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS medicine_lots (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 product_id BIGINT NOT NULL, batch_number VARCHAR(50) NOT NULL,
 expiry_date DATE NOT NULL, received_on DATE NOT NULL,
 unit_cost DECIMAL(12,2) NOT NULL,
 farm_id BIGINT NOT NULL,
 recorded_by BIGINT NULL, recorded_at DATETIME(6) NULL,
 last_changed_by BIGINT NULL, last_changed_at DATETIME(6) NULL,
 KEY idx_med_lot_farm (farm_id),
 CONSTRAINT fk_med_lot_farm FOREIGN KEY (farm_id) REFERENCES farms(id),
 CONSTRAINT fk_med_lot_product FOREIGN KEY (product_id) REFERENCES medicine_products(id),
 CONSTRAINT ck_med_lot CHECK (unit_cost > 0 AND expiry_date >= received_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS medicine_movements (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 lot_id BIGINT NOT NULL, reference_id BIGINT NULL,
 kind VARCHAR(20) NOT NULL, event_date DATE NOT NULL,
 delta INT NOT NULL, unit_price DECIMAL(12,2) NOT NULL,
 party VARCHAR(120) NOT NULL DEFAULT '', notes VARCHAR(300) NOT NULL DEFAULT '',
 request_key CHAR(36) NOT NULL, payload_hash BINARY(32) NOT NULL,
 farm_id BIGINT NOT NULL,
 recorded_by BIGINT NULL, recorded_at DATETIME(6) NULL,
 last_changed_by BIGINT NULL, last_changed_at DATETIME(6) NULL,
 UNIQUE KEY uq_med_request (farm_id,request_key),
 KEY idx_med_date (farm_id,event_date,id),
 CONSTRAINT fk_med_move_farm FOREIGN KEY (farm_id) REFERENCES farms(id),
 CONSTRAINT fk_med_move_lot FOREIGN KEY (lot_id) REFERENCES medicine_lots(id),
 CONSTRAINT fk_med_move_ref FOREIGN KEY (reference_id) REFERENCES medicine_movements(id),
 CONSTRAINT ck_med_move CHECK (delta <> 0 AND unit_price >= 0 AND kind IN ('PURCHASE','SALE','OWN_USE','SALES_RETURN','SUPPLIER_RETURN','DAMAGE','REVERSAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
