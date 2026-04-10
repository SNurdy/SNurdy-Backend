-- V22__add_active_social_id_for_soft_delete.sql

SET @drop_social_unique_idx = (
    SELECT CASE
               WHEN EXISTS(
                   SELECT 1
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'users'
                     AND index_name = 'uk_user_social_id_type_deleted_at'
               ) THEN 'ALTER TABLE users DROP INDEX uk_user_social_id_type_deleted_at'
               WHEN EXISTS(
                   SELECT 1
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'users'
                     AND index_name = 'uk_user_social_id_type'
               ) THEN 'ALTER TABLE users DROP INDEX uk_user_social_id_type'
               ELSE 'SELECT 1'
        END
);
PREPARE stmt_drop_social_unique_idx FROM @drop_social_unique_idx;
EXECUTE stmt_drop_social_unique_idx;
DEALLOCATE PREPARE stmt_drop_social_unique_idx;

SET @add_active_social_id_col = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'active_social_id'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD COLUMN active_social_id VARCHAR(255) GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN social_id ELSE NULL END) STORED'
    )
);
PREPARE stmt_add_active_social_id_col FROM @add_active_social_id_col;
EXECUTE stmt_add_active_social_id_col;
DEALLOCATE PREPARE stmt_add_active_social_id_col;

SET @add_social_active_unique_idx = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND index_name = 'uk_user_social_active'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD UNIQUE KEY uk_user_social_active (active_social_id, social_type)'
    )
);
PREPARE stmt_add_social_active_unique_idx FROM @add_social_active_unique_idx;
EXECUTE stmt_add_social_active_unique_idx;
DEALLOCATE PREPARE stmt_add_social_active_unique_idx;
