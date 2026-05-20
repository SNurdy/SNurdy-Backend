-- V28__create_user_term_memos.sql
-- v2: 단어 카드 사용자별 메모 테이블
-- 한 사용자가 같은 단어에 메모 하나만 가짐 (userId + termId 복합키)

SET @create_user_term_memos = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'user_term_memos'
        ),
        'SELECT 1',
        'CREATE TABLE user_term_memos (
            user_id    BINARY(16)   NOT NULL,
            term_id    BIGINT       NOT NULL,
            content    VARCHAR(500) NOT NULL,
            created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (user_id, term_id),
            CONSTRAINT fk_user_term_memo_user
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            CONSTRAINT fk_user_term_memo_term
                FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
    )
);
PREPARE stmt FROM @create_user_term_memos;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
