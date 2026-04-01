-- V3__add_updated_at_to_relation_tables.sql

-- 1) glossary_terms에 updated_at 추가
ALTER TABLE glossary_terms
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 2) bookmarks에 updated_at 추가
ALTER TABLE bookmarks
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 3) term_morphemes에 created_at, updated_at 추가
ALTER TABLE term_morphemes
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;