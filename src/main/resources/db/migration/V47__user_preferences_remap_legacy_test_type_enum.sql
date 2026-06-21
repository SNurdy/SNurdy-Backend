-- V47__user_preferences_remap_legacy_test_type_enum.sql
-- V46 에서 default_test_type (단일 VARCHAR) 을 JSON_ARRAY(value) 로 wrap 하면서
-- 옛 enum value 가 그대로 1원소 배열로 남은 row 가 있었음
-- Phase 2.13 의 신 enum 7종으로 1:1 매핑 가능한 것은 치환, 모호한 것은 폐기 (빈 배열 = 다음 진입 시 모달 강제)
--
-- 매핑:
--   MEANING_TO_TERM       -> WRITE_TERM_FROM_MEANING       (의미 → 용어)
--   TERM_TO_MEANING       -> WRITE_MEANING_FROM_TERM       (용어 → 의미)
--   WRITE_ABBREVIATION    -> WRITE_ABBREVIATION_FROM_TERM  (용어 → 약어)
--   LISTEN_AND_WRITE      -> LISTEN_AND_WRITE              (그대로)
--   MC_PICK_MEANING       -> MC_MEANING_FROM_TERM          (용어 → 의미 선택)
--   MC_PICK_TERM          -> 폐기 (FROM_ABBREVIATION / FROM_MEANING 판정 불가)
--   MC_PICK_ABBR          -> 폐기 (기획 미사용으로 신규 enum 에 없음)
-- 이미 신 enum 값은 그대로 유지

UPDATE user_preferences
   SET default_test_types = (
     SELECT COALESCE(JSON_ARRAYAGG(mapped), JSON_ARRAY())
       FROM (
         SELECT DISTINCT CASE elem
           WHEN 'MEANING_TO_TERM'    THEN 'WRITE_TERM_FROM_MEANING'
           WHEN 'TERM_TO_MEANING'    THEN 'WRITE_MEANING_FROM_TERM'
           WHEN 'WRITE_ABBREVIATION' THEN 'WRITE_ABBREVIATION_FROM_TERM'
           WHEN 'LISTEN_AND_WRITE'   THEN 'LISTEN_AND_WRITE'
           WHEN 'MC_PICK_MEANING'    THEN 'MC_MEANING_FROM_TERM'
           WHEN 'MC_PICK_TERM'       THEN NULL
           WHEN 'MC_PICK_ABBR'       THEN NULL
           ELSE elem
         END AS mapped
         FROM JSON_TABLE(
           user_preferences.default_test_types,
           '$[*]' COLUMNS (elem VARCHAR(64) PATH '$')
         ) AS j
       ) src
       WHERE mapped IS NOT NULL
   )
 WHERE JSON_OVERLAPS(
   default_test_types,
   JSON_ARRAY('MEANING_TO_TERM','TERM_TO_MEANING','WRITE_ABBREVIATION',
              'MC_PICK_TERM','MC_PICK_MEANING','MC_PICK_ABBR')
 );
