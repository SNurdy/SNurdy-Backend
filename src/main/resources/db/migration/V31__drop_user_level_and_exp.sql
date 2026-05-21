-- V31__drop_user_level_and_exp.sql
-- 사용자 단일 level/exp 컬럼 제거 — 레벨은 단어장별 학습률 기반 계산값으로 전환
-- 컬럼이 없는 환경에서도 동작하도록 IF EXISTS 사용 (MySQL 8.0+ 지원)

ALTER TABLE users DROP COLUMN IF EXISTS level;
ALTER TABLE users DROP COLUMN IF EXISTS exp;
