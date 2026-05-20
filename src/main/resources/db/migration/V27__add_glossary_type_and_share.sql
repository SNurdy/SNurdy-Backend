-- V27__add_glossary_type_and_share.sql
-- v2: 단어장에 type/owner/share_code 추가
-- 기존 단어장은 모두 type='DEFAULT', owner_id=NULL 로 유지

-- 1. type 컬럼 추가 (DEFAULT 가 기본값이라 기존 행은 자동으로 DEFAULT)
SET @add_type = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'glossaries'
              AND column_name = 'type'
        ),
        'SELECT 1',
        'ALTER TABLE glossaries ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT ''DEFAULT'''
    )
);
PREPARE stmt FROM @add_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. owner_id 컬럼 추가 (CUSTOM/WRONG_ANSWER 만 채움)
SET @add_owner_id = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'glossaries'
              AND column_name = 'owner_id'
        ),
        'SELECT 1',
        'ALTER TABLE glossaries ADD COLUMN owner_id BINARY(16) NULL'
    )
);
PREPARE stmt FROM @add_owner_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. share_code 컬럼 추가
SET @add_share_code = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'glossaries'
              AND column_name = 'share_code'
        ),
        'SELECT 1',
        'ALTER TABLE glossaries ADD COLUMN share_code VARCHAR(12) NULL'
    )
);
PREPARE stmt FROM @add_share_code;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. share_code unique index (NULL 여러개는 MySQL이 unique 검사에서 제외)
SET @add_share_code_idx = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'glossaries'
              AND index_name = 'uk_glossary_share_code'
        ),
        'SELECT 1',
        'ALTER TABLE glossaries ADD UNIQUE KEY uk_glossary_share_code (share_code)'
    )
);
PREPARE stmt FROM @add_share_code_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. type 과 owner_id 정합성 강제
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
