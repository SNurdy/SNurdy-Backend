-- V45__rename_daily_progresses_count_columns.sql
-- daily_progresses 의 learned_count / completed_count 의 의미가 모호하다는 평
-- 신규 학습 vs 그 날 전체 학습 의도가 컬럼명에서 드러나지 않아 new_learned_count / all_learned_count 로 정정
-- 값 그대로

SET @rename_learned = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'daily_progresses'
              AND column_name = 'learned_count'
        ),
        'ALTER TABLE daily_progresses CHANGE COLUMN learned_count new_learned_count INT NOT NULL DEFAULT 0',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_learned;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @rename_completed = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'daily_progresses'
              AND column_name = 'completed_count'
        ),
        'ALTER TABLE daily_progresses CHANGE COLUMN completed_count all_learned_count INT NOT NULL DEFAULT 0',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_completed;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
