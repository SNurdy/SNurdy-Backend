-- V40__add_user_default_test_type.sql
-- 사용자 기본 테스트 유형 컬럼 추가
-- null 이면 다음 테스트 진입 시 유형 선택 모달 강제

SET @add_default_test_type = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'default_test_type'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD COLUMN default_test_type VARCHAR(32) NULL'
    )
);
PREPARE stmt FROM @add_default_test_type;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
