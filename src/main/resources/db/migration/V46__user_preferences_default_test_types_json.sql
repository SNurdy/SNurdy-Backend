-- V46__user_preferences_default_test_types_json.sql
-- Phase 2.13 다중 testType 정책에 맞춰 user_preferences.default_test_type 컬럼을 List 로 확장
-- 단일 VARCHAR(32) -> JSON 배열. 기존 row 의 단일 값은 1원소 배열로 변환, NULL 은 빈 배열로 변환

-- 1. 새 컬럼 default_test_types JSON 추가 (NULL 허용, 초기 null)
SET @add_col = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'user_preferences'
                 AND column_name = 'default_test_types'),
        'SELECT 1',
        'ALTER TABLE user_preferences ADD COLUMN default_test_types JSON NULL AFTER default_test_type'
    )
);
PREPARE stmt FROM @add_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 기존 default_test_type 값을 JSON 배열로 변환
-- NULL -> '[]' (빈 배열 = 모달 강제 의미 유지)
-- 단일 값 -> JSON_ARRAY(값)
UPDATE user_preferences
   SET default_test_types = IF(
        default_test_type IS NULL OR default_test_type = '',
        JSON_ARRAY(),
        JSON_ARRAY(default_test_type)
   )
 WHERE default_test_types IS NULL;

-- 3. 옛 컬럼 default_test_type drop (idempotent)
SET @drop_col = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'user_preferences'
                 AND column_name = 'default_test_type'),
        'ALTER TABLE user_preferences DROP COLUMN default_test_type',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
