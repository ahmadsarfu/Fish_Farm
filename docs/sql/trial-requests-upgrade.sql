-- Additive migration. Preserves existing accounts, farms and business data.
CREATE TABLE IF NOT EXISTS trial_requests (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 owner_name VARCHAR(120) NOT NULL,
 farm_name VARCHAR(120) NOT NULL,
 mobile VARCHAR(16) NOT NULL,
 email VARCHAR(254) NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 created_at DATETIME(6) NOT NULL,
 reviewed_at DATETIME(6) NULL,
 reviewed_by BIGINT NULL,
 farm_id BIGINT NULL,
 UNIQUE KEY uq_trial_mobile (mobile),
 KEY idx_trial_status_id (status,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
