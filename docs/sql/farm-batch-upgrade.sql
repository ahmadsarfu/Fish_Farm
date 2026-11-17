-- UPGRADE AN EXISTING DATABASE: select it first (for example USE fish_farm_db;).
-- Back up the database before running. MySQL DDL commits independently.
-- Existing business rows are retained and assigned to legacy farm 1.
-- Re-running this upgrade does not overwrite an existing owner or farm data.

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


-- Preserve existing feed IDs, quantities and rates while adapting older column names.
DELIMITER $$
DROP PROCEDURE IF EXISTS fishfarm_feed_compatibility$$
CREATE PROCEDURE fishfarm_feed_compatibility()
BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='feed_types' AND column_name='pellet_size_mm')
    AND NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='feed_types' AND column_name='size_mm') THEN
  ALTER TABLE feed_types RENAME COLUMN pellet_size_mm TO size_mm;
 END IF;
 IF EXISTS(SELECT 1 FROM feed_types WHERE size_mm NOT IN (1,2,3,4)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing feed sizes must be reviewed: only 1, 2, 3 and 4 mm are supported';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='feed_types' AND column_name='name') THEN
  ALTER TABLE feed_types ADD COLUMN name VARCHAR(100) NULL;
 END IF;
 UPDATE feed_types SET name=CONCAT(size_mm,' mm Feed') WHERE name IS NULL OR TRIM(name)='';
 ALTER TABLE feed_types MODIFY COLUMN name VARCHAR(100) NOT NULL;
END$$
CALL fishfarm_feed_compatibility()$$
DROP PROCEDURE fishfarm_feed_compatibility$$
DELIMITER ;


-- Run after selecting your application database. Take a database backup first.
-- Existing domain records remain in farm 1. Existing legacy users are NOT auto-enabled.
CREATE TABLE IF NOT EXISTS farms (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(120) NOT NULL,
 legacy_claimable BOOLEAN NOT NULL DEFAULT FALSE, active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;
INSERT INTO farms(id,name,legacy_claimable,active)
SELECT 1,'Existing Farm',TRUE,TRUE WHERE NOT EXISTS(SELECT 1 FROM farms WHERE id=1);
CREATE TABLE IF NOT EXISTS app_accounts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(120) NOT NULL UNIQUE,
 display_name VARCHAR(120) NOT NULL, password_hash VARCHAR(100) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE, session_version INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS farm_members (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, farm_id BIGINT NOT NULL, account_id BIGINT NOT NULL,
 role VARCHAR(15) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 UNIQUE KEY uq_farm_member(farm_id,account_id),
 FOREIGN KEY(farm_id) REFERENCES farms(id), FOREIGN KEY(account_id) REFERENCES app_accounts(id),
 CHECK(role IN ('OWNER','MANAGER','LABOUR'))
) ENGINE=InnoDB;

DELIMITER $$
DROP PROCEDURE IF EXISTS fishfarm_add_ownership$$
CREATE PROCEDURE fishfarm_add_ownership(IN table_name_arg VARCHAR(64))
BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=table_name_arg) THEN
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=table_name_arg AND column_name='farm_id') THEN
   SET @sql=CONCAT('ALTER TABLE `',table_name_arg,'` ADD COLUMN farm_id BIGINT NOT NULL DEFAULT 1, ADD INDEX idx_farm_id(farm_id), ADD FOREIGN KEY(farm_id) REFERENCES farms(id)');
   PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=table_name_arg AND column_name='recorded_by') THEN
   SET @sql=CONCAT('ALTER TABLE `',table_name_arg,'` ADD COLUMN recorded_by BIGINT NULL, ADD COLUMN recorded_at DATETIME(6) NULL, ADD COLUMN last_changed_by BIGINT NULL, ADD COLUMN last_changed_at DATETIME(6) NULL');
   PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
 END IF;
END$$
DROP PROCEDURE IF EXISTS fishfarm_local_key$$
CREATE PROCEDURE fishfarm_local_key(IN table_arg VARCHAR(64), IN column_arg VARCHAR(64), IN key_arg VARCHAR(64))
BEGIN
 DECLARE existing_key VARCHAR(64);
 key_loop: LOOP
  SET existing_key=NULL;
  SELECT index_name INTO existing_key FROM information_schema.statistics
   WHERE table_schema=DATABASE() AND table_name=table_arg AND non_unique=0 AND index_name<>'PRIMARY'
   GROUP BY index_name HAVING COUNT(*)=1 AND MAX(column_name)=column_arg LIMIT 1;
  IF existing_key IS NULL THEN LEAVE key_loop; END IF;
  SET @sql=CONCAT('ALTER TABLE `',table_arg,'` DROP INDEX `',existing_key,'`');
  PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
 END LOOP;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=table_arg AND index_name=key_arg) THEN
  SET @sql=CONCAT('ALTER TABLE `',table_arg,'` ADD UNIQUE KEY `',key_arg,'`(farm_id,`',column_arg,'`)');
  PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
 END IF;
END$$
DELIMITER ;
CALL fishfarm_add_ownership('ponds');
CALL fishfarm_add_ownership('fish_types');
CALL fishfarm_add_ownership('feed_types');
CALL fishfarm_add_ownership('pond_feed_configs');
CALL fishfarm_add_ownership('daily_feeds');
CALL fishfarm_add_ownership('feeding_records');
CALL fishfarm_add_ownership('fish_stock_entries');
CALL fishfarm_add_ownership('customers');
CALL fishfarm_add_ownership('dispatches');
CALL fishfarm_add_ownership('weighing_sessions');
CALL fishfarm_add_ownership('weight_entries');
CALL fishfarm_add_ownership('harvests');
CALL fishfarm_add_ownership('harvest_items');
CALL fishfarm_add_ownership('inventory');
CALL fishfarm_add_ownership('expenses');
CALL fishfarm_add_ownership('sales');
CALL fishfarm_add_ownership('sale_items');
CALL fishfarm_add_ownership('dispatch_returns');
CALL fishfarm_add_ownership('audit_logs');
CALL fishfarm_local_key('ponds','pond_number','uq_ponds_farm_key');
CALL fishfarm_local_key('fish_types','name','uq_fish_types_farm_key');
CALL fishfarm_local_key('feed_types','size_mm','uq_feed_types_farm_key');
CALL fishfarm_local_key('harvests','harvest_number','uq_harvests_farm_number');
CALL fishfarm_local_key('dispatches','dispatch_number','uq_dispatches_farm_number');
DROP PROCEDURE fishfarm_add_ownership;
DROP PROCEDURE fishfarm_local_key;
CREATE TABLE IF NOT EXISTS pond_assignments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, farm_id BIGINT NOT NULL, account_id BIGINT NOT NULL, pond_id BIGINT NOT NULL,
 recorded_by BIGINT, recorded_at DATETIME(6), last_changed_by BIGINT, last_changed_at DATETIME(6),
 UNIQUE KEY uq_pond_assignment(farm_id,account_id,pond_id),
 FOREIGN KEY(farm_id) REFERENCES farms(id), FOREIGN KEY(account_id) REFERENCES app_accounts(id), FOREIGN KEY(pond_id) REFERENCES ponds(id)
) ENGINE=InnoDB;


-- Run after farm-auth-foundation.sql, in the same selected database.
-- Additive batch and costing tables. Existing domain data is retained.

CREATE TABLE IF NOT EXISTS `fish_batches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `farm_id` bigint NOT NULL,
  `last_changed_at` datetime(6) DEFAULT NULL,
  `last_changed_by` bigint DEFAULT NULL,
  `recorded_at` datetime(6) DEFAULT NULL,
  `recorded_by` bigint DEFAULT NULL,
  `code` varchar(40) NOT NULL,
  `initial_count` int NOT NULL,
  `name` varchar(120) NOT NULL,
  `purchase_cost` decimal(18,2) NOT NULL,
  `started_on` date NOT NULL,
  `fish_type_id` bigint NOT NULL,
  `stock_entry_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_batch_code` (`farm_id`,`code`),
  UNIQUE KEY `UKec1wi62bgmm648cfm0cb2l3wy` (`stock_entry_id`),
  KEY `FKlrdtinya6ie3sh9s179cuy4v3` (`fish_type_id`),
  CONSTRAINT `FKdn3v5caxmbpoxycgftftc9kcy` FOREIGN KEY (`stock_entry_id`) REFERENCES `fish_stock_entries` (`id`),
  CONSTRAINT `FKlrdtinya6ie3sh9s179cuy4v3` FOREIGN KEY (`fish_type_id`) REFERENCES `fish_types` (`id`),
  CONSTRAINT `fk_fish_batches_farm` FOREIGN KEY (`farm_id`) REFERENCES `farms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `batch_stages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `farm_id` bigint NOT NULL,
  `last_changed_at` datetime(6) DEFAULT NULL,
  `last_changed_by` bigint DEFAULT NULL,
  `recorded_at` datetime(6) DEFAULT NULL,
  `recorded_by` bigint DEFAULT NULL,
  `active_pond_id` bigint GENERATED ALWAYS AS ((case when (`current_count` > 0) then `pond_id` else NULL end)) STORED,
  `current_count` int NOT NULL,
  `dead_count` int NOT NULL,
  `ended_on` date DEFAULT NULL,
  `harvested_cost` decimal(18,2) NOT NULL,
  `harvested_count` int NOT NULL,
  `last_activity_on` date NOT NULL,
  `opening_cost` decimal(18,2) NOT NULL,
  `opening_count` int NOT NULL,
  `stage_number` int NOT NULL,
  `started_on` date NOT NULL,
  `transferred_cost` decimal(18,2) NOT NULL,
  `transferred_count` int NOT NULL,
  `batch_id` bigint NOT NULL,
  `parent_stage_id` bigint DEFAULT NULL,
  `pond_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_active_pond_stage` (`farm_id`,`active_pond_id`),
  KEY `FK7uo01b1lc0f83y39fa2wbghk0` (`batch_id`),
  KEY `FK24u1196ugibnp9jarx1jwq23w` (`parent_stage_id`),
  KEY `FKiobexd0uq2ws2ooblp4i57uqn` (`pond_id`),
  CONSTRAINT `FK24u1196ugibnp9jarx1jwq23w` FOREIGN KEY (`parent_stage_id`) REFERENCES `batch_stages` (`id`),
  CONSTRAINT `FK7uo01b1lc0f83y39fa2wbghk0` FOREIGN KEY (`batch_id`) REFERENCES `fish_batches` (`id`),
  CONSTRAINT `FKiobexd0uq2ws2ooblp4i57uqn` FOREIGN KEY (`pond_id`) REFERENCES `ponds` (`id`),
  CONSTRAINT `fk_batch_stages_farm` FOREIGN KEY (`farm_id`) REFERENCES `farms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `stage_expenses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `farm_id` bigint NOT NULL,
  `last_changed_at` datetime(6) DEFAULT NULL,
  `last_changed_by` bigint DEFAULT NULL,
  `recorded_at` datetime(6) DEFAULT NULL,
  `recorded_by` bigint DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL,
  `category` varchar(20) NOT NULL,
  `expense_date` date NOT NULL,
  `feed_entry_id` bigint DEFAULT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `stage_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6gl59qfhyd8egscqvgecj5610` (`feed_entry_id`),
  KEY `FK6les6mmpunyai18kil0gexwwp` (`stage_id`),
  CONSTRAINT `FK6les6mmpunyai18kil0gexwwp` FOREIGN KEY (`stage_id`) REFERENCES `batch_stages` (`id`),
  CONSTRAINT `fk_stage_expenses_farm` FOREIGN KEY (`farm_id`) REFERENCES `farms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `batch_movements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `farm_id` bigint NOT NULL,
  `last_changed_at` datetime(6) DEFAULT NULL,
  `last_changed_by` bigint DEFAULT NULL,
  `recorded_at` datetime(6) DEFAULT NULL,
  `recorded_by` bigint DEFAULT NULL,
  `allocated_cost` decimal(18,2) NOT NULL,
  `event_date` date NOT NULL,
  `fingerprint` varchar(64) NOT NULL,
  `fish_count` int NOT NULL,
  `harvest_id` bigint DEFAULT NULL,
  `kind` varchar(20) NOT NULL,
  `notes` varchar(500) DEFAULT NULL,
  `request_id` varchar(36) NOT NULL,
  `revenue` decimal(18,2) NOT NULL,
  `weight_kg` decimal(12,3) DEFAULT NULL,
  `stage_id` bigint NOT NULL,
  `target_stage_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_movement_request` (`farm_id`,`request_id`),
  KEY `FKn7elp2njytyjhpnpnrvr9brfy` (`stage_id`),
  KEY `FKkenikbk4kvb3yr9rvnaimg33w` (`target_stage_id`),
  CONSTRAINT `FKkenikbk4kvb3yr9rvnaimg33w` FOREIGN KEY (`target_stage_id`) REFERENCES `batch_stages` (`id`),
  CONSTRAINT `FKn7elp2njytyjhpnpnrvr9brfy` FOREIGN KEY (`stage_id`) REFERENCES `batch_stages` (`id`),
  CONSTRAINT `fk_batch_movements_farm` FOREIGN KEY (`farm_id`) REFERENCES `farms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `farm_audit_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `farm_id` bigint NOT NULL,
  `last_changed_at` datetime(6) DEFAULT NULL,
  `last_changed_by` bigint DEFAULT NULL,
  `recorded_at` datetime(6) DEFAULT NULL,
  `recorded_by` bigint DEFAULT NULL,
  `action` varchar(40) NOT NULL,
  `detail` varchar(1000) NOT NULL,
  `entity_id` bigint DEFAULT NULL,
  `entity_type` varchar(40) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_farm_audit_events_farm` FOREIGN KEY (`farm_id`) REFERENCES `farms` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
