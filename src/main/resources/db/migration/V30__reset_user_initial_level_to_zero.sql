-- V30__reset_user_initial_level_to_zero.sql
-- 사용자 초기 레벨 정책을 0 으로 변경
-- 1. users.level 컬럼 default 를 0 으로 조정
-- 2. 기존 level=1 사용자를 0 으로 reset (v2 레벨 기능 미노출 상태라 데이터 손실 없음)

ALTER TABLE users
    ALTER COLUMN level SET DEFAULT 0;

UPDATE users SET level = 0 WHERE level = 1;
