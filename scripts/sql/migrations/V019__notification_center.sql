USE south_stand;
SET NAMES utf8mb4;

-- T21 notification center: additive only; no existing rows are changed.
CREATE TABLE IF NOT EXISTS notification (
  id BIGINT NOT NULL PRIMARY KEY,
  recipient_user_id BIGINT NOT NULL,
  actor_user_id BIGINT NULL,
  notification_type VARCHAR(32) NOT NULL,
  target_type VARCHAR(32) NULL,
  target_id BIGINT NULL,
  secondary_target_type VARCHAR(32) NULL,
  secondary_target_id BIGINT NULL,
  title VARCHAR(128) NULL,
  content VARCHAR(512) NULL,
  read_flag TINYINT NOT NULL DEFAULT 0,
  read_time DATETIME(3) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  dedup_key VARCHAR(128) NOT NULL,
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_notification_dedup (dedup_key),
  KEY idx_notification_recipient_time (recipient_user_id,create_time),
  KEY idx_notification_recipient_read_time (recipient_user_id,read_flag,create_time),
  KEY idx_notification_target (target_type,target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
