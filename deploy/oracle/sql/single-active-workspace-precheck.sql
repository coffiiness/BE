-- READ ONLY. Run against an explicitly selected database after a reviewed backup.
-- Do not print OAuth tokens, passwords or invitation tokens.
SELECT DATABASE() AS selected_database, VERSION() AS mysql_version;

-- Must return zero rows before applying the UNIQUE index. Includes same-tenant duplicates.
SELECT user_id, COUNT(*) AS active_count,
       GROUP_CONCAT(CONCAT(id, ':', tenant_id) ORDER BY id) AS membership_ids_and_tenants
FROM workspace_members WHERE status = 'ACTIVE'
GROUP BY user_id HAVING COUNT(*) > 1;

-- Complete rows for reviewed resolution; GROUP_CONCAT above may be length-limited.
SELECT m.id, m.user_id, m.tenant_id, m.member_type, m.status
FROM workspace_members m
JOIN (SELECT user_id FROM workspace_members WHERE status = 'ACTIVE'
      GROUP BY user_id HAVING COUNT(*) > 1) duplicates ON duplicates.user_id = m.user_id
WHERE m.status = 'ACTIVE' ORDER BY m.user_id, m.id;

-- Review orphans and inactive parents without automatically repairing them.
SELECT m.id, m.user_id, m.tenant_id, u.status AS user_status, w.status AS workspace_status
FROM workspace_members m
LEFT JOIN users u ON u.id = m.user_id
LEFT JOIN workspaces w ON w.workspace_id = m.tenant_id
WHERE m.status = 'ACTIVE' AND
      (u.id IS NULL OR w.id IS NULL OR u.status <> 'ACTIVE' OR w.status <> 'ACTIVE');

-- Used for last-HR review before approving any membership deactivation.
SELECT w.workspace_id, COUNT(m.id) AS active_hr_count
FROM workspaces w
LEFT JOIN workspace_members m ON m.tenant_id = w.workspace_id
  AND m.status = 'ACTIVE' AND m.member_type = 'HR'
WHERE w.status = 'ACTIVE'
GROUP BY w.workspace_id HAVING COUNT(m.id) <= 1;

-- Suspect calendar connections: metadata only, never access/refresh tokens.
SELECT c.id, c.tenant_id, c.user_id, c.is_sync_enabled,
       c.channel_id, c.channel_resource_id, c.channel_expires_at
FROM external_calendars c
WHERE c.status = 'ACTIVE' AND c.is_sync_enabled = TRUE AND NOT EXISTS (
  SELECT 1 FROM workspace_members m WHERE m.user_id = c.user_id
    AND m.tenant_id = c.tenant_id AND m.status = 'ACTIVE');

SELECT column_name, column_type, extra, generation_expression
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'workspace_members'
  AND column_name = 'active_user_id';
SELECT index_name, non_unique, column_name
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'workspace_members'
  AND index_name = 'uq_workspace_members_active_user';
