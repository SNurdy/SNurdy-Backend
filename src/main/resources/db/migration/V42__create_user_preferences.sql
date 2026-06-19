-- V42__create_user_preferences.sql
-- 사용자 환경 설정 분리 테이블 신설
-- UserEntity 비대화 해소 + selectedGlossaryId 영구화
-- default_test_type 은 V40 에서 users 에 추가했던 컬럼을 이관 (V43 에서 users 컬럼 drop)
-- selected_glossary_id 는 NOT NULL DEFAULT 6 (심장내과). 자동 선택 모드 없음, 항상 명시값

SET @create_table = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = 'user_preferences'
        ),
        'SELECT 1',
        'CREATE TABLE user_preferences (
            user_id BINARY(16) NOT NULL,
            default_test_type VARCHAR(32) NULL,
            selected_glossary_id BIGINT NOT NULL DEFAULT 6,
            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
            updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
            PRIMARY KEY (user_id),
            CONSTRAINT fk_user_preferences_user
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            CONSTRAINT fk_user_preferences_glossary
                FOREIGN KEY (selected_glossary_id) REFERENCES glossaries(id) ON DELETE RESTRICT
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
    )
);
PREPARE stmt FROM @create_table;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 유저 일괄 INSERT
-- default_test_type 은 users 에서 가져오고, selected_glossary_id 는 6 (심장내과) 일괄 세팅
-- ON DUPLICATE KEY UPDATE 로 재실행 안전성 확보
INSERT INTO user_preferences (user_id, default_test_type, selected_glossary_id, created_at, updated_at)
SELECT u.id, u.default_test_type, 6, NOW(6), NOW(6)
FROM users u
ON DUPLICATE KEY UPDATE user_id = user_preferences.user_id;
