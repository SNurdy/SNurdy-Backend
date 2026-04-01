-- V18__create_migration_sessions.sql

CREATE TABLE migration_sessions (
                                    id          BINARY(16)   NOT NULL COMMENT 'migrationId (서버 발급 UUID)',
                                    user_id     BINARY(16)   NOT NULL COMMENT 'users.id',
                                    status      VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS'
                                        COMMENT 'IN_PROGRESS, COMPLETED, FAILED',
                                    payload_size INT          NULL     COMMENT '업로드된 payload 바이트 수',
                                    error_message VARCHAR(500) NULL    COMMENT '실패 사유',
                                    completed_at  TIMESTAMP   NULL     COMMENT '완료 시각',
                                    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                    PRIMARY KEY (id),
                                    UNIQUE KEY uk_migration_user (user_id),
                                    CONSTRAINT fk_migration_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
