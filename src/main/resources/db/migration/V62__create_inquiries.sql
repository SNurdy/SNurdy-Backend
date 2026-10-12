-- V62__create_inquiries.sql
-- 1대1 문의 본문
-- 사용자 물리 삭제가 생겨도 문의는 보존하기 위해 user_id 는 ON DELETE SET NULL

CREATE TABLE IF NOT EXISTS inquiries (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    user_id    BINARY(16)    NULL COMMENT '작성 사용자 UUID',
    category   VARCHAR(30)   NOT NULL COMMENT 'InquiryCategory enum name',
    content    VARCHAR(1000) NOT NULL COMMENT '문의 내용 utf8mb4 문자 단위 1000자',
    status     VARCHAR(20)   NOT NULL COMMENT 'WAITING 또는 ANSWERED',
    version    BIGINT        NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전',
    deleted_at TIMESTAMP     NULL COMMENT '사용자 삭제 시각 NULL 이면 활성',
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_inquiries_user_created (user_id, created_at DESC),
    INDEX idx_inquiries_status_created (status, created_at),
    CONSTRAINT fk_inquiries_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;