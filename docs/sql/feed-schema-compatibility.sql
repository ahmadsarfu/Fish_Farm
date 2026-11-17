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
