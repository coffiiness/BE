-- MANUAL migration for MySQL 8.4; never run by application startup or CI deployment.
-- Stop membership writes, review precheck, resolve duplicates with owners, and back up first.
-- DDL implicitly commits. Run WITHOUT mysql --force; stop immediately on any error.
-- Intentionally not idempotent: inspect partial application before a reviewed retry.
DELIMITER $$
CREATE PROCEDURE calfit_require_single_active_membership()
BEGIN
  IF EXISTS (SELECT user_id FROM workspace_members WHERE status = 'ACTIVE'
             GROUP BY user_id HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate ACTIVE memberships: reviewed cleanup required';
  END IF;
END$$
DELIMITER ;
CALL calfit_require_single_active_membership();
DROP PROCEDURE calfit_require_single_active_membership;

ALTER TABLE workspace_members
  ADD COLUMN active_user_id BIGINT
    GENERATED ALWAYS AS (CASE WHEN status = 'ACTIVE' THEN user_id ELSE NULL END) VIRTUAL,
  ADD CONSTRAINT uq_workspace_members_active_user UNIQUE (active_user_id);

CREATE TABLE google_calendar_oauth_states (
  id BIGINT NOT NULL AUTO_INCREMENT,
  status ENUM('ACTIVE', 'DELETED'),
  created_at DATETIME(6),
  updated_at DATETIME(6),
  state_hash VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  workspace_id VARCHAR(255) NOT NULL,
  redirect_uri VARCHAR(1024) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  used_at DATETIME(6),
  PRIMARY KEY (id),
  UNIQUE KEY uq_google_calendar_oauth_state_hash (state_hash),
  KEY ix_google_calendar_oauth_state_user_workspace (user_id, workspace_id)
) ENGINE=InnoDB;

-- Hibernate validate checks column types, not the uniqueness/expression invariant.
SELECT column_type, extra, generation_expression FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'workspace_members' AND column_name = 'active_user_id';
SELECT index_name, non_unique, column_name FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'workspace_members'
  AND index_name = 'uq_workspace_members_active_user';
