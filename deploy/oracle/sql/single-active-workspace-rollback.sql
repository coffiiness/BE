-- MANUAL only, after stopping membership/OAuth writes and rolling application code back.
-- Prefer leaving this additive constraint in place when rolling the application back.
-- Removing it permits duplicates again; obtain explicit operational approval first.
ALTER TABLE workspace_members
  DROP INDEX uq_workspace_members_active_user,
  DROP COLUMN active_user_id;
-- Keep google_calendar_oauth_states and all membership/calendar histories intact.
-- Do not delete users, DELETED memberships, calendar tokens or Google calendars.
