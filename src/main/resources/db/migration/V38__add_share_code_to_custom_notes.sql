-- custom_notes 에 share_code 컬럼 통합. custom_note_share_codes 별도 테이블 폐기 예정 (V39)
-- 노트와 공유 코드의 1:1 lifetime 이라 컬럼으로 흡수
-- share_code IS NULL = 공유 비활성. 비활성화는 별도 컬럼 없이 null 처리

SET @add_share_code = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'custom_notes'
              AND column_name = 'share_code'
        ),
        'SELECT 1',
        'ALTER TABLE custom_notes ADD COLUMN share_code VARCHAR(12) NULL COMMENT ''8자 공유 코드. NULL 이면 비활성'''
    )
);
PREPARE stmt FROM @add_share_code;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_share_code_idx = (
    SELECT IF(
        EXISTS(
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'custom_notes'
              AND index_name = 'uk_custom_note_share_code'
        ),
        'SELECT 1',
        'ALTER TABLE custom_notes ADD UNIQUE KEY uk_custom_note_share_code (share_code)'
    )
);
PREPARE stmt FROM @add_share_code_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
