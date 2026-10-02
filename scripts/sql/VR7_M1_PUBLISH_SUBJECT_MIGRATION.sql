USE south_stand;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS content_publish_subject (
  id BIGINT NOT NULL PRIMARY KEY,
  subject_type VARCHAR(32) NOT NULL,
  name VARCHAR(128) NOT NULL,
  summary VARCHAR(512) NULL,
  cover_url VARCHAR(512) NULL,
  hot_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_publish_subject_type_name (subject_type, name),
  KEY idx_publish_subject_list (subject_type, status, is_deleted, hot_score, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
