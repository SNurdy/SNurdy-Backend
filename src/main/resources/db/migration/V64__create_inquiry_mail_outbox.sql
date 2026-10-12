-- V64__create_inquiry_mail_outbox.sql
-- 개발팀 메일 발송 대장 Transactional Outbox
-- 문의가 지워져도 발송 이력은 남기기 위해 inquiries FK 를 두지 않음

CREATE TABLE IF NOT EXISTS inquiry_mail_outbox (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    inquiry_id    BIGINT        NOT NULL COMMENT '대상 문의 ID',
    mail_type     VARCHAR(20)   NOT NULL COMMENT 'CREATED 또는 UPDATED',
    category      VARCHAR(30)   NOT NULL COMMENT '발송 시점 문의 유형 스냅샷',
    content       VARCHAR(1000) NOT NULL COMMENT '발송 시점 문의 내용 스냅샷',
    status        VARCHAR(20)   NOT NULL COMMENT 'PENDING PUBLISHING PUBLISHED SENT FAILED',
    attempt_count INT           NOT NULL DEFAULT 0 COMMENT 'consumer 처리 시도 횟수',
    last_error    VARCHAR(500)  NULL COMMENT '마지막 실패 사유',
    published_at  TIMESTAMP     NULL COMMENT 'SQS 발행 시각',
    sent_at       TIMESTAMP     NULL COMMENT 'SES 발송 성공 시각',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_inquiry_mail_outbox_status_created (status, created_at),
    INDEX idx_inquiry_mail_outbox_inquiry (inquiry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;