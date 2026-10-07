-- V63__create_inquiry_answers.sql
-- 문의 답변 현재는 문의 1건당 답변 1건
-- 대화형으로 확장할 때는 uk_inquiry_answers_inquiry 만 제거

CREATE TABLE IF NOT EXISTS inquiry_answers (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    inquiry_id    BIGINT        NOT NULL COMMENT '대상 문의 ID',
    admin_user_id BINARY(16)    NOT NULL COMMENT '답변한 관리자 UUID',
    content       VARCHAR(2000) NOT NULL COMMENT '답변 내용 utf8mb4 문자 단위 2000자',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inquiry_answers_inquiry (inquiry_id),
    CONSTRAINT fk_inquiry_answers_inquiry FOREIGN KEY (inquiry_id) REFERENCES inquiries(id),
    CONSTRAINT fk_inquiry_answers_admin FOREIGN KEY (admin_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;