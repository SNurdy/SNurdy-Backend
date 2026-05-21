-- V32__rename_daily_progresses_reviewed_count.sql
-- daily_progresses.reviewed_count 가 사실은 "하루 완료한 학습 이벤트 총수" 라서
-- LearningSubmitResponse.reviewedCount (진짜 복습 수) 와 의미 충돌이 발생했다.
-- 컬럼명을 completed_count 로 정정해 의미를 명확히 한다. 데이터 값은 그대로.

SET @rename_col = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'daily_progresses'
              AND column_name = 'reviewed_count'
        ),
        'ALTER TABLE daily_progresses CHANGE COLUMN reviewed_count completed_count INT NOT NULL DEFAULT 0',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
