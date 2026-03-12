-- V16__drop_actor_uuid_columns.sql
ALTER TABLE admin_audit_logs
DROP COLUMN actor_user_id;

ALTER TABLE change_requests
DROP COLUMN requested_by,
    DROP COLUMN applied_by,
    DROP COLUMN rolled_back_by;