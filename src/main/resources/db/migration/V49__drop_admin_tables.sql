-- V49__drop_admin_tables.sql
-- admin 도메인 제거에 따라 미사용 관리자 테이블 삭제
-- 세 테이블 모두 다른 테이블이 참조하지 않는 leaf 이므로 FK 해제 없이 삭제 가능

DROP TABLE IF EXISTS change_requests;
DROP TABLE IF EXISTS admin_audit_logs;
DROP TABLE IF EXISTS admin_users;
