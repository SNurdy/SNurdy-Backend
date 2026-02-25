-- V12__add_rollback_columns_to_change_requests.sql

ALTER TABLE change_requests
    ADD COLUMN rolled_back_by BINARY(16) NULL COMMENT '롤백 수행자 (users.id)' AFTER applied_at,
    ADD COLUMN rolled_back_at TIMESTAMP  NULL COMMENT '롤백 시각' AFTER rolled_back_by,
    ADD CONSTRAINT fk_cr_rolled_back_by FOREIGN KEY (rolled_back_by) REFERENCES users(id) ON DELETE SET NULL;