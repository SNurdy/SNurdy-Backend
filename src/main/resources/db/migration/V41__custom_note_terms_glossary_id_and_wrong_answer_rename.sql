-- custom_note_terms 에 glossary_id 컬럼 추가 + PK 확장 (note_id, glossary_id, term_id)
-- wrong_answer_terms.source_glossary_id -> glossary_id 컬럼명 일관화

-- 1. custom_note_terms.glossary_id 컬럼 추가 (NULL 로 시작 — 기존 row 채우기 위해)
SET @add_glossary_col = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'custom_note_terms'
                 AND column_name = 'glossary_id'),
        'SELECT 1',
        'ALTER TABLE custom_note_terms ADD COLUMN glossary_id BIGINT NULL AFTER note_id'
    )
);
PREPARE stmt FROM @add_glossary_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 기존 row 의 glossary_id 임시 채움 — 단어가 매핑된 단어장 중 최소 id (임의 1개)
UPDATE custom_note_terms cnt
SET glossary_id = (
    SELECT MIN(gt.glossary_id)
    FROM glossary_terms gt
    WHERE gt.term_id = cnt.term_id
)
WHERE cnt.glossary_id IS NULL;

-- 3. glossary_id NOT NULL 변경 (idempotent)
SET @to_not_null = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'custom_note_terms'
                 AND column_name = 'glossary_id'
                 AND is_nullable = 'NO'),
        'SELECT 1',
        'ALTER TABLE custom_note_terms MODIFY COLUMN glossary_id BIGINT NOT NULL'
    )
);
PREPARE stmt FROM @to_not_null;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. PK 재구성 — (note_id, term_id) -> (note_id, glossary_id, term_id)
SET @reconstruct_pk = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'custom_note_terms'
                 AND index_name = 'PRIMARY'
                 AND column_name = 'glossary_id'),
        'SELECT 1',
        'ALTER TABLE custom_note_terms DROP PRIMARY KEY, ADD PRIMARY KEY (note_id, glossary_id, term_id)'
    )
);
PREPARE stmt FROM @reconstruct_pk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. glossary_id FK 추가
SET @add_fk_glossary = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.referential_constraints
               WHERE constraint_schema = DATABASE()
                 AND constraint_name = 'fk_custom_note_term_glossary'),
        'SELECT 1',
        'ALTER TABLE custom_note_terms ADD CONSTRAINT fk_custom_note_term_glossary FOREIGN KEY (glossary_id) REFERENCES glossaries(id) ON DELETE CASCADE'
    )
);
PREPARE stmt FROM @add_fk_glossary;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. wrong_answer_terms.source_glossary_id -> glossary_id rename (nullable 유지)
SET @rename_source_glossary = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = 'wrong_answer_terms'
                 AND column_name = 'source_glossary_id'),
        'ALTER TABLE wrong_answer_terms CHANGE COLUMN source_glossary_id glossary_id BIGINT NULL COMMENT ''마지막 오답 발생 시점의 학습 단어장 ID. soft ref''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_source_glossary;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 7. wrong_answer_terms 의 인덱스 이름도 source -> glossary 로 일관화
-- 기존 idx_wrong_answer_user_source -> idx_wrong_answer_user_glossary
SET @rename_index = (
    SELECT IF(
        EXISTS(SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'wrong_answer_terms'
                 AND index_name = 'idx_wrong_answer_user_source'),
        'ALTER TABLE wrong_answer_terms RENAME INDEX idx_wrong_answer_user_source TO idx_wrong_answer_user_glossary',
        'SELECT 1'
    )
);
PREPARE stmt FROM @rename_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
