-- V44__add_default_card_count_to_user_preferences.sql
-- 사용자별 한 세션 학습 카드 수 default
-- 허용 값 10, 20, 30, 40, 50. 학습 Home 셀렉터 초기값으로 사용

SET @add_default_card_count = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'user_preferences'
              AND column_name = 'default_card_count'
        ),
        'SELECT 1',
        'ALTER TABLE user_preferences ADD COLUMN default_card_count INT NOT NULL DEFAULT 10'
    )
);
PREPARE stmt FROM @add_default_card_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
