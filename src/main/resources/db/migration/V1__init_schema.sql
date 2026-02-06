-- =====================
-- SNurdy 초기 스키마
-- =====================

-- 사용자
CREATE TABLE users (
        id BINARY(16) PRIMARY KEY,
        social_id VARCHAR(255) NOT NULL,
        social_type VARCHAR(20) NOT NULL,
        email VARCHAR(255),
        name VARCHAR(50),
        nickname VARCHAR(20) NOT NULL DEFAULT '',
        role VARCHAR(20) NOT NULL DEFAULT 'USER',
        current_streak INT NOT NULL DEFAULT 0,
        longest_streak INT NOT NULL DEFAULT 0,
        total_learning_terms INT NOT NULL DEFAULT 0,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        deleted_at TIMESTAMP NULL,
        UNIQUE KEY uk_user_social_id_type (social_id, social_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 학습 기록 (캘린더용)
CREATE TABLE date_infos (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BINARY(16) NOT NULL,
        date DATE NOT NULL,
        study_count INT NOT NULL DEFAULT 0 COMMENT '해당 날짜 학습한 단어 수',
        review_count INT NOT NULL DEFAULT 0 COMMENT '해당 날짜 복습한 단어 수',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uk_date_info_user_date (user_id, date),
        CONSTRAINT fk_date_info_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 단어장 (카테고리)
CREATE TABLE glossaries (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        category VARCHAR(50) NOT NULL COMMENT '내과, 외과, 기타',
        title VARCHAR(100) NOT NULL COMMENT '일반내과, 호흡기내과 등',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 단어장 진행도
CREATE TABLE glossary_progresses (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BINARY(16) NOT NULL,
        glossary_id BIGINT NOT NULL,
        studied_count INT NOT NULL DEFAULT 0,
        last_studied_at TIMESTAMP NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uk_glossary_progress (user_id, glossary_id),
        CONSTRAINT fk_glossary_progress_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        CONSTRAINT fk_glossary_progress_glossary FOREIGN KEY (glossary_id) REFERENCES glossaries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 단어 (공용 데이터)
CREATE TABLE terms (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        glossary_id BIGINT NOT NULL,
        spelling VARCHAR(100) NOT NULL COMMENT '철자',
        meaning VARCHAR(255) NOT NULL COMMENT '기본 뜻',
        abbreviation VARCHAR(50) COMMENT '약어',
        explanation TEXT COMMENT '자세한 설명',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        CONSTRAINT fk_term_glossary FOREIGN KEY (glossary_id) REFERENCES glossaries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 형태소
CREATE TABLE morphemes (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        spelling VARCHAR(50) NOT NULL COMMENT '철자 ex) 폐',
        meaning VARCHAR(100) NOT NULL COMMENT '의미',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 단어-형태소 관계
CREATE TABLE term_morpheme_relationships (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        term_id BIGINT NOT NULL,
        morpheme_id BIGINT NOT NULL,
        position INT NOT NULL COMMENT '형태소 순서',
        UNIQUE KEY uk_term_position (term_id, position),
        CONSTRAINT fk_tmr_term FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE,
        CONSTRAINT fk_tmr_morpheme FOREIGN KEY (morpheme_id) REFERENCES morphemes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 개인 학습 데이터 (SRS)
CREATE TABLE term_study_datas (
        id BINARY(16) PRIMARY KEY,
        user_id BINARY(16) NOT NULL,
        term_id BIGINT NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT 'NEW' COMMENT 'NEW, LEARNING, REVIEW, MASTERED',
        ease_factor DECIMAL(4,2) NOT NULL DEFAULT 2.50,
        interval_days INT NOT NULL DEFAULT 0,
        review_count INT NOT NULL DEFAULT 0,
        last_reviewed_at TIMESTAMP NULL,
        next_review_at TIMESTAMP NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uk_term_study_user_term (user_id, term_id),
        INDEX idx_term_study_next_review (user_id, next_review_at),
        CONSTRAINT fk_term_study_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        CONSTRAINT fk_term_study_term FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;