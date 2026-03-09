-- V14__create_admin_users_table.sql
-- 관리자 전용 계정 테이블 (users 테이블과 독립)

CREATE TABLE admin_users (
                             id BINARY(16) PRIMARY KEY,
                             email VARCHAR(255) NOT NULL,
                             password VARCHAR(255) NOT NULL COMMENT 'BCrypt 해시',
                             name VARCHAR(50) NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             UNIQUE KEY uk_admin_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;