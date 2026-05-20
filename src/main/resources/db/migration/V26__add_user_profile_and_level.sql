-- V23__add_user_profile_and_level.sql
-- v2: 마이페이지 프로필 이미지/레벨/마케팅 동의 컬럼 추가
-- 기존 사용자는 모두 level=1, exp=0, marketing_consent=false 로 시작

SET @add_profile_image_url = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'profile_image_url'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD COLUMN profile_image_url VARCHAR(512) NULL'
    )
);
PREPARE stmt FROM @add_profile_image_url;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_level = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'level'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD COLUMN level INT NOT NULL DEFAULT 1'
    )
);
PREPARE stmt FROM @add_level;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_exp = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'exp'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD COLUMN exp INT NOT NULL DEFAULT 0'
    )
);
PREPARE stmt FROM @add_exp;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_marketing_consent = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'marketing_consent'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD COLUMN marketing_consent BOOLEAN NOT NULL DEFAULT FALSE'
    )
);
PREPARE stmt FROM @add_marketing_consent;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_marketing_consent_at = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'marketing_consent_at'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD COLUMN marketing_consent_at TIMESTAMP NULL'
    )
);
PREPARE stmt FROM @add_marketing_consent_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
