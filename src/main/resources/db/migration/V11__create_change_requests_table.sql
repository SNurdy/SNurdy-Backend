-- V11__create_change_requests_table.sql

-- 1. 기존 테이블 FK 해제 후 삭제
ALTER TABLE admin_audit_logs DROP FOREIGN KEY fk_admin_audit_logs_change_set;
DROP TABLE IF EXISTS change_set_items;
DROP TABLE IF EXISTS change_sets;

-- 2. 신규 테이블 생성
CREATE TABLE change_requests (
                                 id              BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 title           VARCHAR(150)  NOT NULL COMMENT '변경 요청 제목',
                                 description     VARCHAR(1000) NULL     COMMENT '변경 요청 설명',

    -- 상태: REQUESTED → VALIDATED → APPLIED / FAILED / CANCELED
                                 status          VARCHAR(30)   NOT NULL DEFAULT 'REQUESTED',

    -- 대상 엔티티 정보
                                 entity_type     VARCHAR(50)   NOT NULL COMMENT 'TERM, MORPHEME',
                                 operation       VARCHAR(20)   NOT NULL COMMENT 'CREATE, UPDATE, DELETE',
                                 entity_pk       VARCHAR(100)  NOT NULL DEFAULT 'NEW' COMMENT '대상 PK (신규는 NEW, 반영 후 실제 PK)',

    -- 변경 데이터
                                 before_json     JSON          NULL     COMMENT '변경 전 스냅샷',
                                 after_json      JSON          NULL     COMMENT '변경 후 데이터',

    -- 검증 결과
                                 error_code      VARCHAR(100)  NULL     COMMENT '실패 코드',
                                 error_message   VARCHAR(500)  NULL     COMMENT '실패 상세 메시지',
                                 warning_message VARCHAR(1000) NULL     COMMENT '검증 경고 (통과했지만 주의 필요)',

    -- 행위자
                                 requested_by    BINARY(16)    NOT NULL COMMENT '요청자 (users.id)',
                                 applied_by      BINARY(16)    NULL     COMMENT '반영자 (users.id)',
                                 applied_at      TIMESTAMP     NULL,

                                 created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                 INDEX idx_cr_status_created (status, created_at DESC),
                                 INDEX idx_cr_entity (entity_type, entity_pk),
                                 INDEX idx_cr_requested_by (requested_by, created_at DESC),

                                 CONSTRAINT fk_cr_requested_by FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE RESTRICT,
                                 CONSTRAINT fk_cr_applied_by   FOREIGN KEY (applied_by)   REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;