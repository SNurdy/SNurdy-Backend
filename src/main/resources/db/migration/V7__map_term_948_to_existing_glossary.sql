-- V7__map_term_948_to_existing_glossary.sql
-- glossary에 매핑되지 않은 term(948, vomiting)을 기존 중복 용어(586)와 동일 glossary에 매핑

INSERT INTO glossary_terms (glossary_id, term_id)
SELECT gt.glossary_id, 948
FROM glossary_terms gt
JOIN terms t_source ON t_source.id = 586
JOIN terms t_target ON t_target.id = 948
WHERE gt.term_id = 586
  AND NOT EXISTS (
    SELECT 1
    FROM glossary_terms existing
    WHERE existing.glossary_id = gt.glossary_id
      AND existing.term_id = 948
  );