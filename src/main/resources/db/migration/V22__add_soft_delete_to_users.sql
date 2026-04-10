-- V22__add_active_social_id_for_soft_delete.sql

ALTER TABLE users
DROP INDEX uk_user_social_id_type_deleted_at;

ALTER TABLE users
    ADD UNIQUE KEY uk_user_social_active (active_social_id, social_type);