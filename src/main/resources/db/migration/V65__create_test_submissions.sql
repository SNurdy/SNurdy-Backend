-- 시험 제출 멱등성 키별 최초 처리 응답 저장
-- 동일 키 재요청 시 재처리 없이 저장된 응답 반환

CREATE TABLE IF NOT EXISTS test_submissions (
                                                user_id         BINARY(16)  NOT NULL
    COMMENT '제출 사용자 UUID',

    idempotency_key BINARY(16)  NOT NULL
    COMMENT '앱이 시험마다 생성한 UUID',

    submission_type VARCHAR(40) NOT NULL
    COMMENT 'GLOSSARY, WRONG_ANSWER_NOTE_BY_PERIOD, WRONG_ANSWER_NOTE_BY_GLOSSARY',

    response_body   JSON        NOT NULL
    COMMENT '최초 처리 응답 data',

    created_at      TIMESTAMP   NOT NULL
    DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, idempotency_key),

    INDEX idx_test_submissions_created (created_at),

    CONSTRAINT fk_test_submissions_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB
    DEFAULT CHARSET=utf8mb4
    COLLATE=utf8mb4_unicode_ci;