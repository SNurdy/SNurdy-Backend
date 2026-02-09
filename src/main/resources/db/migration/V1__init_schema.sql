-- 1. 사용자
CREATE TABLE users (
        id BINARY(16) PRIMARY KEY,
        social_id VARCHAR(255) NOT NULL,
        social_type VARCHAR(20) NOT NULL,
        email VARCHAR(255),
        name VARCHAR(50),
        nickname VARCHAR(20) NOT NULL DEFAULT '',
        role VARCHAR(20) NOT NULL DEFAULT 'USER',
        current_streak INT NOT NULL DEFAULT 0 COMMENT '현재 연속 학습일',
        longest_streak INT NOT NULL DEFAULT 0 COMMENT '최대 연속 학습일',
        total_learning_terms INT NOT NULL DEFAULT 0 COMMENT '총 학습 단어 수',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        deleted_at TIMESTAMP NULL,
        UNIQUE KEY uk_user_social (social_id, social_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 학습 기록 (캘린더용)
CREATE TABLE date_infos (
        user_id BINARY(16) NOT NULL,
        date DATE NOT NULL,
        study_count INT NOT NULL DEFAULT 0 COMMENT '해당 날짜 학습한 단어 수',
        review_count INT NOT NULL DEFAULT 0 COMMENT '해당 날짜 복습한 단어 수',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (user_id, date),
        CONSTRAINT fk_date_info_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 단어장
CREATE TABLE glossaries (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        category VARCHAR(50) NOT NULL COMMENT '내과, 외과, 기타',
        title VARCHAR(100) NOT NULL COMMENT '일반내과, 호흡기내과 등',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 단어 (공용 데이터)
CREATE TABLE terms (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        spelling VARCHAR(100) NOT NULL COMMENT '철자',
        meaning VARCHAR(255) NOT NULL COMMENT '기본 뜻',
        abbreviation VARCHAR(50) COMMENT '약어',
        explanation TEXT COMMENT '자세한 설명',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 단어장-단어 관계 (N:N)
CREATE TABLE glossary_terms (
        glossary_id BIGINT NOT NULL,
        term_id BIGINT NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (glossary_id, term_id),
        CONSTRAINT fk_gt_glossary FOREIGN KEY (glossary_id) REFERENCES glossaries(id) ON DELETE CASCADE,
        CONSTRAINT fk_gt_term FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 형태소
CREATE TABLE morphemes (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        spelling VARCHAR(50) NOT NULL COMMENT '철자 ex) hepat(o)-',
        meaning VARCHAR(100) NOT NULL COMMENT '의미 ex) 간',
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 단어-형태소 관계 (N:N)
CREATE TABLE term_morphemes (
        term_id BIGINT NOT NULL,
        morpheme_id BIGINT NOT NULL,
        position INT NOT NULL COMMENT '형태소 순서 (0부터 시작)',
        PRIMARY KEY (term_id, position),
        CONSTRAINT fk_tm_term FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE,
        CONSTRAINT fk_tm_morpheme FOREIGN KEY (morpheme_id) REFERENCES morphemes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 단어장 진행도
CREATE TABLE glossary_progresses (
        user_id BINARY(16) NOT NULL,
        glossary_id BIGINT NOT NULL,
        studied_count INT NOT NULL DEFAULT 0 COMMENT '학습한 단어 수',
        last_studied_at TIMESTAMP NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (user_id, glossary_id),
        CONSTRAINT fk_gp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        CONSTRAINT fk_gp_glossary FOREIGN KEY (glossary_id) REFERENCES glossaries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. 개인 학습 데이터
CREATE TABLE term_study_datas (
        id BINARY(16) PRIMARY KEY,
        user_id BINARY(16) NOT NULL,
        glossary_id BIGINT NOT NULL,
        term_id BIGINT NOT NULL,
        status VARCHAR(20) NOT NULL DEFAULT 'NEW' COMMENT 'NEW, LEARNING, REVIEW, MASTERED',
        ease_factor DECIMAL(4,2) NOT NULL DEFAULT 2.50,
        interval_days INT NOT NULL DEFAULT 0,
        review_count INT NOT NULL DEFAULT 0,
        last_reviewed_at TIMESTAMP NULL,
        next_review_at TIMESTAMP NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        UNIQUE KEY uk_study_user_glossary_term (user_id, glossary_id, term_id),
        INDEX idx_study_next_review (user_id, next_review_at),
        CONSTRAINT fk_study_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        CONSTRAINT fk_study_glossary FOREIGN KEY (glossary_id) REFERENCES glossaries(id) ON DELETE CASCADE,
        CONSTRAINT fk_study_term FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. 유저 북마크
CREATE TABLE user_bookmarks (
        user_id BINARY(16) NOT NULL,
        glossary_id BIGINT NOT NULL,
        term_id BIGINT NOT NULL,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (user_id, glossary_id, term_id),
        CONSTRAINT fk_bookmark_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        CONSTRAINT fk_bookmark_glossary FOREIGN KEY (glossary_id) REFERENCES glossaries(id) ON DELETE CASCADE,
        CONSTRAINT fk_bookmark_term FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;