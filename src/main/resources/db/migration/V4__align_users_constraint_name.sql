-- V4__align_users_constraint_name.sql

ALTER TABLE users
DROP INDEX uk_user_social,
    ADD CONSTRAINT uk_user_social_id_type UNIQUE (social_id, social_type);
