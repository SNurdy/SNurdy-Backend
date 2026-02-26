-- V13__add_deleted_at_to_terms.sql

ALTER TABLE terms
    ADD COLUMN deleted_at TIMESTAMP NULL COMMENT '소프트 삭제 시각 (NULL이면 활성)';

-- 단어장 조회 시 소프트 삭제된 단어 제외를 위한 인덱스
CREATE INDEX idx_terms_deleted_at ON terms (deleted_at);