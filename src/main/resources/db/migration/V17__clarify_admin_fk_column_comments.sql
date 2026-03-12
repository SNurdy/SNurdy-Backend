-- V17__clarify_admin_fk_column_comments.sql
ALTER TABLE change_requests
    MODIFY COLUMN applied_at     TIMESTAMP(6) NULL    COMMENT '반영 시각',
    MODIFY COLUMN rolled_back_at TIMESTAMP(6) NULL    COMMENT '롤백 시각';