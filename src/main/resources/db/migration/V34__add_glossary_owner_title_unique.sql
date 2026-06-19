-- V34__add_glossary_owner_title_unique.sql
-- 같은 사용자가 동일 title 의 단어장을 두 개 만드는 것을 DB 레벨에서 차단한다.
-- type=DEFAULT 는 owner_id 가 NULL 이라 MySQL UNIQUE 제약의 NULL 중복 허용 동작으로 영향 없음.

SET @add_unique = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'glossaries'
              AND index_name = 'uk_glossary_owner_title'
        ),
        'SELECT 1',
        'ALTER TABLE glossaries ADD CONSTRAINT uk_glossary_owner_title UNIQUE (owner_id, title)'
    )
);
PREPARE stmt FROM @add_unique;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
