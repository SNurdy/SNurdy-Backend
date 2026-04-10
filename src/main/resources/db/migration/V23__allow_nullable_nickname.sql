-- V23__allow_nullable_nickname.sql

ALTER TABLE users
    MODIFY COLUMN nickname VARCHAR(20) NULL;

UPDATE users SET nickname = NULL WHERE nickname = '';