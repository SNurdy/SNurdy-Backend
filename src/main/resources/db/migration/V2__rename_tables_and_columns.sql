-- V2__rename_tables_and_columns.sql
-- 테이블명 및 컬럼명 변경

-- 1. date_infos → daily_records
RENAME TABLE date_infos TO daily_records;

ALTER TABLE daily_records
    CHANGE COLUMN study_count learned_count INT NOT NULL DEFAULT 0 COMMENT '학습한 단어 수',
    CHANGE COLUMN review_count reviewed_count INT NOT NULL DEFAULT 0 COMMENT '복습한 단어 수';

-- 2. glossary_progresses 컬럼명 변경
ALTER TABLE glossary_progresses
    CHANGE COLUMN studied_count learned_count INT NOT NULL DEFAULT 0 COMMENT '학습한 단어 수',
    CHANGE COLUMN last_studied_at last_learned_at TIMESTAMP NULL;

-- 3. term_study_datas → term_progresses
RENAME TABLE term_study_datas TO term_progresses;

-- 인덱스 및 제약조건 이름 변경
ALTER TABLE term_progresses
DROP INDEX uk_study_user_glossary_term,
    ADD UNIQUE KEY uk_term_progress_user_glossary_term (user_id, glossary_id, term_id);

ALTER TABLE term_progresses
DROP INDEX idx_study_next_review,
    ADD INDEX idx_term_progress_next_review (user_id, next_review_at);

-- 4. user_bookmarks → bookmarks
RENAME TABLE user_bookmarks TO bookmarks;