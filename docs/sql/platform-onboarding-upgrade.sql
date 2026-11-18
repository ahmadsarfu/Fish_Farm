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
-- Some installations manually created platformAdmin and 100-character contacts.
-- Validate before any column changes; never truncate contacts or guess conflicting admin flags.
DELIMITER $$
DROP PROCEDURE IF EXISTS fishfarm_platform_account_check$$
CREATE PROCEDURE fishfarm_platform_account_check()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='app_accounts') THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='app_accounts is missing. Select the correct database and run the farm/auth migration first.';
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='app_accounts' AND column_name='email') THEN
  IF EXISTS(SELECT email FROM app_accounts WHERE email IS NOT NULL GROUP BY email HAVING COUNT(*)>1) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate emails exist. Review them before adding the unique index; no contacts have been removed.';
  END IF;
  IF EXISTS(SELECT 1 FROM app_accounts WHERE CHAR_LENGTH(email)>254) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='An email exceeds 254 characters. Review it before continuing; no value has been truncated.';
  END IF;
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='app_accounts' AND column_name='mobile') THEN
  IF EXISTS(SELECT mobile FROM app_accounts WHERE mobile IS NOT NULL GROUP BY mobile HAVING COUNT(*)>1) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate mobile numbers exist. Review them before adding the unique index; no contacts have been removed.';
  END IF;
  IF EXISTS(SELECT 1 FROM app_accounts WHERE CHAR_LENGTH(mobile)>16) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='A mobile value exceeds 16 characters. Review it before continuing; no value has been truncated.';
  END IF;
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='app_accounts' AND column_name='platformAdmin') THEN
  IF EXISTS(SELECT 1 FROM app_accounts WHERE platformAdmin IS NOT NULL AND platformAdmin NOT IN (0,1)) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected platformAdmin value. Review admin permissions before continuing.';
  END IF;
  IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='app_accounts' AND column_name='platform_admin') THEN
   IF EXISTS(SELECT 1 FROM app_accounts WHERE COALESCE(platformAdmin,0)<>COALESCE(platform_admin,0)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='platformAdmin and platform_admin disagree. Review admin permissions before continuing.';
   END IF;
  END IF;
 END IF;
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='app_accounts' AND column_name='platform_admin') THEN
  IF EXISTS(SELECT 1 FROM app_accounts WHERE platform_admin IS NOT NULL AND platform_admin NOT IN (0,1)) THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected platform_admin value. Review admin permissions before continuing.';
  END IF;
 END IF;
END$$
DELIMITER ;
CALL fishfarm_platform_account_check();
DROP PROCEDURE fishfarm_platform_account_check;
DELIMITER $$
DROP PROCEDURE IF EXISTS fishfarm_platform_admin_name$$
CREATE PROCEDURE fishfarm_platform_admin_name()
BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='app_accounts' AND column_name='platformAdmin')
    AND NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='app_accounts' AND column_name='platform_admin') THEN
  ALTER TABLE app_accounts RENAME COLUMN platformAdmin TO platform_admin;
 END IF;
END$$
DELIMITER ;
CALL fishfarm_platform_admin_name();
DROP PROCEDURE fishfarm_platform_admin_name;
CALL fishfarm_platform_column('app_accounts','email','VARCHAR(254) NULL');
CALL fishfarm_platform_column('app_accounts','mobile','VARCHAR(16) NULL');
CALL fishfarm_platform_column('app_accounts','platform_admin','BOOLEAN NOT NULL DEFAULT FALSE');
UPDATE app_accounts SET platform_admin=FALSE WHERE platform_admin IS NULL;
ALTER TABLE app_accounts
 MODIFY COLUMN email VARCHAR(254) NULL,
 MODIFY COLUMN mobile VARCHAR(16) NULL,
 MODIFY COLUMN platform_admin BOOLEAN NOT NULL DEFAULT FALSE;
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
