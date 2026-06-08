-- V43__drop_user_default_test_type.sql
-- users.default_test_type 컬럼 제거 (V42 에서 user_preferences 로 이관 완료)

SET @drop_column = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'default_test_type'
        ),
        'ALTER TABLE users DROP COLUMN default_test_type',
        'SELECT 1'
    )
);
PREPARE stmt FROM @drop_column;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
