-- V55__merge_nonstandard_synonyms.sql
-- 비표준 동의어를 정배 간호용어로 병합. 참조 7테이블 repoint 후 소프트삭제
-- 흉막천자[376]->흉강천자[193], 폐색전[177]->폐색전증[364], 울혈심부전[224,744]->울혈성심부전[25]

-- 흉막천자 -> 흉강천자
UPDATE IGNORE term_progresses SET term_id=193 WHERE term_id IN (376);
DELETE FROM term_progresses WHERE term_id IN (376);
UPDATE IGNORE bookmarks SET term_id=193 WHERE term_id IN (376);
DELETE FROM bookmarks WHERE term_id IN (376);
UPDATE IGNORE wrong_answer_terms SET term_id=193 WHERE term_id IN (376);
DELETE FROM wrong_answer_terms WHERE term_id IN (376);
UPDATE IGNORE custom_note_terms SET term_id=193 WHERE term_id IN (376);
DELETE FROM custom_note_terms WHERE term_id IN (376);
UPDATE IGNORE glossary_terms SET term_id=193 WHERE term_id IN (376);
DELETE FROM glossary_terms WHERE term_id IN (376);
UPDATE IGNORE term_morphemes SET term_id=193 WHERE term_id IN (376);
DELETE FROM term_morphemes WHERE term_id IN (376);
UPDATE IGNORE user_term_memos SET term_id=193 WHERE term_id IN (376);
DELETE FROM user_term_memos WHERE term_id IN (376);
UPDATE terms SET deleted_at=NOW() WHERE id IN (376) AND deleted_at IS NULL;

-- 폐색전 -> 폐색전증
UPDATE IGNORE term_progresses SET term_id=364 WHERE term_id IN (177);
DELETE FROM term_progresses WHERE term_id IN (177);
UPDATE IGNORE bookmarks SET term_id=364 WHERE term_id IN (177);
DELETE FROM bookmarks WHERE term_id IN (177);
UPDATE IGNORE wrong_answer_terms SET term_id=364 WHERE term_id IN (177);
DELETE FROM wrong_answer_terms WHERE term_id IN (177);
UPDATE IGNORE custom_note_terms SET term_id=364 WHERE term_id IN (177);
DELETE FROM custom_note_terms WHERE term_id IN (177);
UPDATE IGNORE glossary_terms SET term_id=364 WHERE term_id IN (177);
DELETE FROM glossary_terms WHERE term_id IN (177);
UPDATE IGNORE term_morphemes SET term_id=364 WHERE term_id IN (177);
DELETE FROM term_morphemes WHERE term_id IN (177);
UPDATE IGNORE user_term_memos SET term_id=364 WHERE term_id IN (177);
DELETE FROM user_term_memos WHERE term_id IN (177);
UPDATE terms SET deleted_at=NOW() WHERE id IN (177) AND deleted_at IS NULL;

-- 울혈심부전 -> 울혈성 심부전
UPDATE IGNORE term_progresses SET term_id=25 WHERE term_id IN (224,744);
DELETE FROM term_progresses WHERE term_id IN (224,744);
UPDATE IGNORE bookmarks SET term_id=25 WHERE term_id IN (224,744);
DELETE FROM bookmarks WHERE term_id IN (224,744);
UPDATE IGNORE wrong_answer_terms SET term_id=25 WHERE term_id IN (224,744);
DELETE FROM wrong_answer_terms WHERE term_id IN (224,744);
UPDATE IGNORE custom_note_terms SET term_id=25 WHERE term_id IN (224,744);
DELETE FROM custom_note_terms WHERE term_id IN (224,744);
UPDATE IGNORE glossary_terms SET term_id=25 WHERE term_id IN (224,744);
DELETE FROM glossary_terms WHERE term_id IN (224,744);
UPDATE IGNORE term_morphemes SET term_id=25 WHERE term_id IN (224,744);
DELETE FROM term_morphemes WHERE term_id IN (224,744);
UPDATE IGNORE user_term_memos SET term_id=25 WHERE term_id IN (224,744);
DELETE FROM user_term_memos WHERE term_id IN (224,744);
UPDATE terms SET deleted_at=NOW() WHERE id IN (224,744) AND deleted_at IS NULL;

