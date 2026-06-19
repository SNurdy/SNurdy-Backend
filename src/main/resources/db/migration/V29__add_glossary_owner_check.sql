-- V29__add_glossary_owner_check.sql
-- v2: type 과 owner_id 정합성 강제
-- DEFAULT 는 owner_id 가 반드시 NULL
-- CUSTOM/WRONG_ANSWER 는 owner_id 가 반드시 NOT NULL
-- MySQL 8.0.16 이상에서 CHECK 제약이 실제로 강제됨

SET @add_check_owner = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.table_constraints
            WHERE table_schema = DATABASE()
              AND table_name = 'glossaries'
              AND constraint_name = 'chk_glossary_owner_by_type'
        ),
        'SELECT 1',
        'ALTER TABLE glossaries ADD CONSTRAINT chk_glossary_owner_by_type CHECK ((type = ''DEFAULT'' AND owner_id IS NULL) OR (type IN (''CUSTOM'', ''WRONG_ANSWER'') AND owner_id IS NOT NULL))'
    )
);
PREPARE stmt FROM @add_check_owner;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
