-- V31__drop_user_level_and_exp.sql
-- 사용자 단일 level/exp 컬럼 제거 — 레벨은 단어장별 학습률 기반 계산값으로 전환
-- MySQL 은 ALTER TABLE DROP COLUMN IF EXISTS 를 지원하지 않으므로 information_schema 로 동적 분기

SET @drop_level = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'level'
        ),
        'ALTER TABLE users DROP COLUMN level',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_level;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_exp = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'exp'
        ),
        'ALTER TABLE users DROP COLUMN exp',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_exp;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
