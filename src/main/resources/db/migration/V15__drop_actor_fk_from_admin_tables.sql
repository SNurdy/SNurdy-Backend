-- V15__drop_actor_fk_from_admin_tables.sql

ALTER TABLE change_requests DROP FOREIGN KEY fk_cr_requested_by;
ALTER TABLE change_requests DROP FOREIGN KEY fk_cr_applied_by;
ALTER TABLE change_requests DROP FOREIGN KEY fk_cr_rolled_back_by;

ALTER TABLE admin_audit_logs DROP FOREIGN KEY fk_admin_audit_logs_actor;