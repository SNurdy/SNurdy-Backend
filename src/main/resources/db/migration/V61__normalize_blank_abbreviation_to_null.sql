-- V61__normalize_blank_abbreviation_to_null.sql
UPDATE terms
SET abbreviation = NULL
WHERE abbreviation = '';