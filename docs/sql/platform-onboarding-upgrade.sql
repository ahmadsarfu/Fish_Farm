-- Select your existing application database and take a backup first.
-- Run AFTER farm-batch-upgrade.sql. Repeatable; preserves accounts and farm data.
DELIMITER $$
DROP PROCEDURE IF EXISTS fishfarm_platform_column$$
CREATE PROCEDURE fishfarm_platform_column(IN table_arg VARCHAR(64), IN column_arg VARCHAR(64), IN definition_arg VARCHAR(512))
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=table_arg AND column_name=column_arg) THEN
  SET @platform_ddl=CONCAT('ALTER TABLE `',table_arg,'` ADD COLUMN `',column_arg,'` ',definition_arg);
  PREPARE platform_stmt FROM @platform_ddl; EXECUTE platform_stmt; DEALLOCATE PREPARE platform_stmt;
 END IF;
END$$
DELIMITER ;
CALL fishfarm_platform_column('app_accounts','email','VARCHAR(254) NULL');
CALL fishfarm_platform_column('app_accounts','mobile','VARCHAR(16) NULL');
CALL fishfarm_platform_column('app_accounts','platform_admin','BOOLEAN NOT NULL DEFAULT FALSE');
CALL fishfarm_platform_column('farms','plan_name','VARCHAR(80) NOT NULL DEFAULT ''Legacy''');
CALL fishfarm_platform_column('farms','subscription_status','VARCHAR(12) NOT NULL DEFAULT ''ACTIVE''');
CALL fishfarm_platform_column('farms','subscription_ends_on','DATE NULL');
CALL fishfarm_platform_column('farms','managed_subscription','BOOLEAN NOT NULL DEFAULT FALSE');
CALL fishfarm_platform_column('farms','pond_limit','INT NULL');
CALL fishfarm_platform_column('farms','user_limit','INT NULL');
DROP PROCEDURE fishfarm_platform_column;
DELIMITER $$
DROP PROCEDURE IF EXISTS fishfarm_platform_indexes$$
CREATE PROCEDURE fishfarm_platform_indexes()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='app_accounts' AND column_name='email' AND non_unique=0) THEN
  ALTER TABLE app_accounts ADD UNIQUE KEY uq_accounts_email(email);
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='app_accounts' AND column_name='mobile' AND non_unique=0) THEN
  ALTER TABLE app_accounts ADD UNIQUE KEY uq_accounts_mobile(mobile);
 END IF;
END$$
DELIMITER ;
CALL fishfarm_platform_indexes();
DROP PROCEDURE fishfarm_platform_indexes;
CREATE TABLE IF NOT EXISTS customer_invitations (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 farm_id BIGINT NOT NULL UNIQUE, account_id BIGINT NOT NULL UNIQUE,
 token_hash VARCHAR(64) NOT NULL UNIQUE,
 expires_at DATETIME(6) NOT NULL, accepted_at DATETIME(6) NULL,
 token_consumed_at DATETIME(6) NULL, purpose VARCHAR(20) NOT NULL DEFAULT 'ACTIVATION',
 trial_days INT NOT NULL,
 FOREIGN KEY(farm_id) REFERENCES farms(id),
 FOREIGN KEY(account_id) REFERENCES app_accounts(id)
) ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS platform_audit_events (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 actor_id BIGINT NOT NULL, farm_id BIGINT NOT NULL,
 action VARCHAR(40) NOT NULL, occurred_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

-- Additive support for an installation that already has the invitation table.
DELIMITER $$
DROP PROCEDURE IF EXISTS fishfarm_invitation_upgrade$$
CREATE PROCEDURE fishfarm_invitation_upgrade()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='customer_invitations' AND column_name='purpose') THEN
  ALTER TABLE customer_invitations ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'ACTIVATION';
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='customer_invitations' AND column_name='token_consumed_at') THEN
  ALTER TABLE customer_invitations ADD COLUMN token_consumed_at DATETIME(6) NULL;
 END IF;
END$$
DELIMITER ;
CALL fishfarm_invitation_upgrade();
DROP PROCEDURE fishfarm_invitation_upgrade;
