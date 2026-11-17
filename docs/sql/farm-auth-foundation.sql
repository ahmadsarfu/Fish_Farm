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
