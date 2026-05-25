-- 오답노트 단어 (사용자 x 단어 단위, soft delete)
CREATE TABLE IF NOT EXISTS wrong_answer_terms (
    user_id BINARY(16) NOT NULL,
    term_id BIGINT NOT NULL,
    source_glossary_id BIGINT NULL COMMENT '마지막 오답 발생 시점의 학습 단어장 ID. soft ref',
    incorrect_at TIMESTAMP NOT NULL COMMENT '마지막 오답 발생 시각',
    total_incorrect_count INT NOT NULL DEFAULT 0 COMMENT '누적 오답 횟수. 자체 누적해 SUM 회피',
    deleted_at TIMESTAMP NULL COMMENT '사용자 수동 삭제 시각. NULL = 활성',
    PRIMARY KEY (user_id, term_id),
    INDEX idx_wrong_answer_user_incorrect
        (user_id, deleted_at, incorrect_at),
    INDEX idx_wrong_answer_user_source
        (user_id, deleted_at, source_glossary_id, incorrect_at),
    CONSTRAINT fk_wrong_answer_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wrong_answer_term
        FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
