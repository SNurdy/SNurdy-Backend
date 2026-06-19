-- 커스텀 단어장 도메인 entity 분리. CUSTOM 데이터를 glossaries 에서 떼어내기 위함
-- glossaries 테이블의 type/owner_id/share_code cleanup 은 V37 에서 수행

-- 1. custom_notes: 사용자 커스텀 단어장
CREATE TABLE IF NOT EXISTS custom_notes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BINARY(16) NOT NULL COMMENT '단어장 소유 사용자 UUID',
    title VARCHAR(14) NOT NULL COMMENT '단어장 제목. 사용자 필수 입력. utf8mb4 character 단위 14자',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_custom_note_owner_title (owner_id, title),
    INDEX idx_custom_note_owner (owner_id, created_at DESC),
    CONSTRAINT fk_custom_note_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. custom_note_terms: CustomNote x Term 매핑
CREATE TABLE IF NOT EXISTS custom_note_terms (
    note_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (note_id, term_id),
    INDEX idx_custom_note_term_term (term_id) COMMENT '단어 기준 역방향 조회용',
    CONSTRAINT fk_custom_note_term_note FOREIGN KEY (note_id) REFERENCES custom_notes(id) ON DELETE CASCADE,
    CONSTRAINT fk_custom_note_term_term FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. custom_note_share_codes: 공유 코드 1:1
-- revoked_at NOT NULL 이면 비활성. 재발급 시 같은 row 덮어쓰기
CREATE TABLE IF NOT EXISTS custom_note_share_codes (
    note_id BIGINT NOT NULL,
    share_code VARCHAR(12) NOT NULL COMMENT '8자 영숫자 코드. 컬럼은 향후 확장 대비 12자',
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL COMMENT '비활성 시각. NULL = 활성',
    PRIMARY KEY (note_id),
    UNIQUE KEY uk_share_code (share_code),
    CONSTRAINT fk_share_code_note FOREIGN KEY (note_id) REFERENCES custom_notes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
