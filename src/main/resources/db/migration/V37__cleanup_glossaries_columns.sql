-- glossaries 테이블의 v2 단계 컬럼 cleanup
-- CUSTOM/WRONG_ANSWER 이 별도 테이블로 분리된 후 type/owner_id/share_code 컬럼은 무의미
-- glossaries 는 사전 카탈로그 (DEFAULT) 전용으로 정리

-- 1. CHECK 제약 제거 (V29 의 chk_glossary_owner_by_type)
SET @drop_check = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.check_constraints
            WHERE constraint_schema = DATABASE()
              AND constraint_name = 'chk_glossary_owner_by_type'
        ),
        'ALTER TABLE glossaries DROP CHECK chk_glossary_owner_by_type',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_check;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. share_code UNIQUE 인덱스 제거 (V27)
SET @drop_share_code_idx = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'glossaries'
              AND index_name = 'uk_glossary_share_code'
        ),
        'ALTER TABLE glossaries DROP INDEX uk_glossary_share_code',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_share_code_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. type 컬럼 제거
SET @drop_type = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'glossaries'
              AND column_name = 'type'
        ),
        'ALTER TABLE glossaries DROP COLUMN type',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. owner_id 컬럼 제거
SET @drop_owner = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'glossaries'
              AND column_name = 'owner_id'
        ),
        'ALTER TABLE glossaries DROP COLUMN owner_id',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_owner;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. share_code 컬럼 제거
SET @drop_share_code = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'glossaries'
              AND column_name = 'share_code'
        ),
        'ALTER TABLE glossaries DROP COLUMN share_code',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_share_code;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
