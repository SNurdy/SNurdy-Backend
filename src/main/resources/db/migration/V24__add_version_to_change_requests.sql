-- JPA @Version 낙관적 락 컬럼 추가. 기존 row는 0으로 초기화.
ALTER TABLE change_requests
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
