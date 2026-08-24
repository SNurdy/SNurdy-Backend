-- V48__normalize_user_name_blank_to_null.sql
-- 값 없음의 표현을 null 하나로 통일. name 이 blank(빈 문자열 또는 공백만) 인 행을 null 로 정규화
-- name 컬럼은 V1 부터 nullable 이라 ALTER 불필요. 재실행 시 대상 0건이라 idempotent
-- 되돌릴 수 없음. '' 를 null 로 바꾸면 원래 null 이던 행과 구분이 사라짐

UPDATE users SET name = NULL
    WHERE name IS NOT NULL AND TRIM(name) = '';
