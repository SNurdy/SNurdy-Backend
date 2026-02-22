-- V10__create_admin_change_management_tables.sql
-- 관리자 CMS 변경관리용 테이블 생성

CREATE TABLE change_sets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL COMMENT '변경 요청 제목',
    description VARCHAR(1000) NULL COMMENT '변경 요청 설명',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT, VALIDATED, APPROVED, APPLIED_DEV, APPLIED_PROD, FAILED, CANCELED',
    source_env VARCHAR(20) NOT NULL DEFAULT 'DEV' COMMENT '변경 생성 환경',
    target_env VARCHAR(20) NOT NULL DEFAULT 'PROD' COMMENT '최종 반영 대상 환경',
    requested_by BINARY(16) NOT NULL COMMENT '요청자(users.id)',
    approved_by BINARY(16) NULL COMMENT '승인자(users.id)',
    approved_at TIMESTAMP NULL,
    applied_by BINARY(16) NULL COMMENT '적용자(users.id)',
    applied_at TIMESTAMP NULL,
    validation_summary JSON NULL COMMENT '검증 결과 요약',
    apply_summary JSON NULL COMMENT '적용 결과 요약',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_change_sets_status_created (status, created_at),
    INDEX idx_change_sets_requested_by_created (requested_by, created_at),
    CONSTRAINT fk_change_sets_requested_by
        FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_change_sets_approved_by
        FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_change_sets_applied_by
        FOREIGN KEY (applied_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE change_set_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    change_set_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL COMMENT 'TERM, GLOSSARY, MORPHEME, TERM_MORPHEME, GLOSSARY_TERM',
    entity_pk VARCHAR(100) NOT NULL COMMENT '대상 레코드 식별자(복합키는 문자열 직렬화)',
    operation VARCHAR(20) NOT NULL COMMENT 'CREATE, UPDATE, DELETE, LINK, UNLINK',
    before_json JSON NULL COMMENT '변경 전 데이터',
    after_json JSON NULL COMMENT '변경 후 데이터',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, VALIDATED, APPLIED, FAILED, SKIPPED',
    error_code VARCHAR(100) NULL COMMENT '실패 코드',
    error_message VARCHAR(500) NULL COMMENT '실패 메시지',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '적용 순서',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_change_set_items_change_set (change_set_id),
    INDEX idx_change_set_items_status (status),
    INDEX idx_change_set_items_entity (entity_type, entity_pk),
    CONSTRAINT fk_change_set_items_change_set
        FOREIGN KEY (change_set_id) REFERENCES change_sets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE admin_audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BINARY(16) NULL COMMENT '행위자(users.id)',
    actor_role VARCHAR(30) NOT NULL COMMENT '행위 당시 역할',
    action VARCHAR(30) NOT NULL COMMENT 'CREATE, UPDATE, DELETE, VALIDATE, APPROVE, APPLY, ROLLBACK, LOGIN, LOGOUT',
    target_table VARCHAR(50) NOT NULL COMMENT '대상 테이블명',
    target_pk VARCHAR(100) NOT NULL COMMENT '대상 PK(복합키는 문자열 직렬화)',
    change_set_id BIGINT NULL COMMENT '관련 변경 요청 ID',
    request_id VARCHAR(64) NULL COMMENT '요청 추적 ID',
    before_json JSON NULL COMMENT '변경 전 데이터',
    after_json JSON NULL COMMENT '변경 후 데이터',
    ip_address VARCHAR(45) NULL COMMENT '요청 IP(IPv4/IPv6)',
    user_agent VARCHAR(255) NULL COMMENT '요청 User-Agent',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_admin_audit_logs_actor_created (actor_user_id, created_at),
    INDEX idx_admin_audit_logs_target (target_table, target_pk),
    INDEX idx_admin_audit_logs_change_set (change_set_id),
    INDEX idx_admin_audit_logs_created (created_at),
    CONSTRAINT fk_admin_audit_logs_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_admin_audit_logs_change_set
        FOREIGN KEY (change_set_id) REFERENCES change_sets(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
