-- Run while feeding writes are paused, before deploying the feature backend.
-- Additive and repeatable. Existing rows become the original rollout snapshot.
DELIMITER $$
DROP PROCEDURE IF EXISTS upgrade_feed_record_integrity$$
CREATE PROCEDURE upgrade_feed_record_integrity()
BEGIN
IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='daily_feeds' AND column_name='original_quantity_kg') THEN
 ALTER TABLE daily_feeds ADD COLUMN original_quantity_kg DECIMAL(10,3) NULL;
END IF;
IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='daily_feeds' AND column_name='original_rate_per_kg') THEN
 ALTER TABLE daily_feeds ADD COLUMN original_rate_per_kg DECIMAL(10,2) NULL;
END IF;
IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='daily_feeds' AND column_name='original_expense') THEN
 ALTER TABLE daily_feeds ADD COLUMN original_expense DECIMAL(18,2) NULL;
END IF;
IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='daily_feeds' AND column_name='status') THEN
 ALTER TABLE daily_feeds ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'RECORDED';
END IF;
IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='daily_feeds' AND column_name='source') THEN
 ALTER TABLE daily_feeds ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'LEGACY';
END IF;
IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='daily_feeds' AND column_name='version') THEN
 ALTER TABLE daily_feeds ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
END IF;
UPDATE daily_feeds SET original_quantity_kg=quantity_kg WHERE original_quantity_kg IS NULL;
UPDATE daily_feeds SET original_rate_per_kg=rate_per_kg WHERE original_rate_per_kg IS NULL;
UPDATE daily_feeds SET original_expense=expense WHERE original_expense IS NULL;
ALTER TABLE daily_feeds MODIFY original_quantity_kg DECIMAL(10,3) NOT NULL,
 MODIFY original_rate_per_kg DECIMAL(10,2) NOT NULL,
 MODIFY original_expense DECIMAL(18,2) NOT NULL;
CREATE TABLE IF NOT EXISTS feed_revisions (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 farm_id BIGINT NOT NULL,
 feed_id BIGINT NOT NULL,
 revision BIGINT NOT NULL,
 action VARCHAR(16) NOT NULL,
 before_quantity_kg DECIMAL(10,3) NOT NULL,
 after_quantity_kg DECIMAL(10,3) NOT NULL,
 before_expense DECIMAL(18,2) NOT NULL,
 after_expense DECIMAL(18,2) NOT NULL,
 adjustment DECIMAL(18,2) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 actor_id BIGINT NOT NULL,
 actor_name VARCHAR(120) NOT NULL,
 reviewed_at DATETIME(6) NOT NULL,
 recorded_by BIGINT NULL,
 recorded_at DATETIME(6) NULL,
 last_changed_by BIGINT NULL,
 last_changed_at DATETIME(6) NULL,
 UNIQUE KEY uq_feed_revision (feed_id, revision),
 KEY idx_revision_farm_feed (farm_id, feed_id),
 CONSTRAINT fk_revision_farm FOREIGN KEY (farm_id) REFERENCES farms(id),
 CONSTRAINT fk_revision_feed FOREIGN KEY (feed_id) REFERENCES daily_feeds(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
END$$
CALL upgrade_feed_record_integrity()$$
DROP PROCEDURE upgrade_feed_record_integrity$$
DELIMITER ;
