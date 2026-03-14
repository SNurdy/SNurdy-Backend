-- V19__add_migration_columns_to_term_progresses.sql

ALTER TABLE term_progresses
    ADD COLUMN client_generated_id VARCHAR(36) NULL COMMENT '클라이언트 생성 UUID (마이그레이션 중복 판정용)',
    ADD COLUMN occurred_at         TIMESTAMP   NULL COMMENT '클라이언트 제공 학습 시각';
