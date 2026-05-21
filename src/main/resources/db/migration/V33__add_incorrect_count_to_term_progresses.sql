-- V33__add_incorrect_count_to_term_progresses.sql
-- 단어별 오답 누적 횟수를 기록. 자주 틀린 단어 분석 / 오답노트 우선순위에 활용 예정.

SET @add_incorrect_count = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'term_progresses'
              AND column_name = 'incorrect_count'
        ),
        'SELECT 1',
        'ALTER TABLE term_progresses ADD COLUMN incorrect_count INT NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @add_incorrect_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
