-- V21__create_app_versions_table.sql

CREATE TABLE app_versions (
                              id         BIGINT PRIMARY KEY AUTO_INCREMENT,
                              platform   VARCHAR(20)  NOT NULL COMMENT 'IOS, ANDROID',
                              latest_version  VARCHAR(20)  NOT NULL COMMENT '최신 버전 (semver)',
                              minimum_version VARCHAR(20)  NOT NULL COMMENT '최소 지원 버전 (이하 강제 업데이트)',
                              update_message       VARCHAR(500) NULL COMMENT '권장 업데이트 메시지',
                              force_update_message VARCHAR(500) NULL COMMENT '강제 업데이트 메시지',
                              store_url  VARCHAR(500) NULL COMMENT '스토어 URL',
                              created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              UNIQUE KEY uk_app_versions_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 초기 데이터 삽입
INSERT INTO app_versions (platform, latest_version, minimum_version, update_message, force_update_message, store_url)
VALUES ('IOS', '1.0.2', '1.0.0',
        '새로운 학습 기능이 추가되었어요!',
        '이 버전은 더 이상 지원하지 않습니다. 업데이트 후 이용해주세요.',
        'https://apps.apple.com/app/snurdy/id...');