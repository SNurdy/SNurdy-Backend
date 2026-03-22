ALTER TABLE users
    ADD COLUMN migration_declined_at TIMESTAMP NULL COMMENT '사용자 마이그레이션 거부 시각(UTC)';
