USE south_stand;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS admin_operation_log;
DROP TABLE IF EXISTS file_resource;
DROP TABLE IF EXISTS match_report;
DROP TABLE IF EXISTS match_event;
DROP TABLE IF EXISTS match_info;
DROP TABLE IF EXISTS team_player;
DROP TABLE IF EXISTS football_player;
DROP TABLE IF EXISTS football_team;
DROP TABLE IF EXISTS football_league;
DROP TABLE IF EXISTS follow_record;
DROP TABLE IF EXISTS favorite_record;
DROP TABLE IF EXISTS like_record;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS content_block;
DROP TABLE IF EXISTS content_relation;
DROP TABLE IF EXISTS content_media;
DROP TABLE IF EXISTS content;
DROP TABLE IF EXISTS user_onboarding;
DROP TABLE IF EXISTS user_profile;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
  id BIGINT NOT NULL PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  phone VARCHAR(32) NULL,
  email VARCHAR(128) NULL,
  password_hash VARCHAR(255) NOT NULL,
  role_type VARCHAR(32) NOT NULL DEFAULT 'USER',
  last_login_time DATETIME NULL,
  onboarding_completed TINYINT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_phone (phone),
  KEY idx_role_status (role_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE file_resource (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NULL,
  biz_type VARCHAR(32) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  storage_name VARCHAR(128) NOT NULL,
  object_key VARCHAR(255) NOT NULL,
  relative_path VARCHAR(255) NOT NULL,
  url VARCHAR(512) NOT NULL,
  content_type VARCHAR(128) NOT NULL,
  extension VARCHAR(16) NOT NULL,
  size_bytes BIGINT NOT NULL,
  storage_type VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
  bucket VARCHAR(128) NULL,
  endpoint VARCHAR(255) NULL,
  public_domain VARCHAR(255) NULL,
  etag VARCHAR(128) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_file_object_key (object_key),
  KEY idx_file_user_id (user_id),
  KEY idx_file_biz_type (biz_type),
  KEY idx_file_created_at (created_at),
  KEY idx_file_status (status),
  KEY idx_file_storage_type (storage_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_profile (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  nickname VARCHAR(64) NOT NULL,
  avatar_url VARCHAR(512) NULL,
  bio VARCHAR(512) NULL,
  main_team_id BIGINT NULL,
  post_count INT NOT NULL DEFAULT 0,
  follower_count INT NOT NULL DEFAULT 0,
  following_count INT NOT NULL DEFAULT 0,
  team_follow_count INT NOT NULL DEFAULT 0,
  player_follow_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_id (user_id),
  KEY idx_main_team_id (main_team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_onboarding (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  main_team_id BIGINT NULL,
  selected_team_ids JSON NULL,
  selected_player_ids JSON NULL,
  completed TINYINT NOT NULL DEFAULT 0,
  completed_time DATETIME NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content (
  id BIGINT NOT NULL PRIMARY KEY,
  content_type VARCHAR(32) NOT NULL,
  content_format VARCHAR(32) NOT NULL DEFAULT 'POST_FORMAT',
  card_type VARCHAR(32) NOT NULL DEFAULT 'CONTENT_CARD',
  title VARCHAR(255) NOT NULL,
  summary VARCHAR(512) NULL,
  body TEXT NULL,
  cover_url VARCHAR(512) NULL,
  author_id BIGINT NOT NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'USER',
  source_name VARCHAR(128) NULL,
  is_official TINYINT NOT NULL DEFAULT 0,
  view_count INT NOT NULL DEFAULT 0,
  like_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  favorite_count INT NOT NULL DEFAULT 0,
  hot_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  publish_time DATETIME NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_type_status_time (content_type, status, publish_time),
  KEY idx_card_status_time (card_type, status, publish_time),
  KEY idx_author_status_time (author_id, status, publish_time),
  KEY idx_hot_time (status, hot_score, publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_media (
  id BIGINT NOT NULL PRIMARY KEY,
  content_id BIGINT NOT NULL,
  media_type VARCHAR(32) NOT NULL,
  media_url VARCHAR(512) NOT NULL,
  thumbnail_url VARCHAR(512) NULL,
  width INT NULL,
  height INT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_content_id (content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_block (
  id BIGINT NOT NULL PRIMARY KEY,
  content_id BIGINT NOT NULL,
  block_type VARCHAR(32) NOT NULL,
  text_content TEXT NULL,
  media_file_id BIGINT NULL,
  media_url VARCHAR(512) NULL,
  embed_url VARCHAR(1024) NULL,
  sort_order INT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_content_block_content_id (content_id),
  KEY idx_content_block_sort (content_id, sort_order),
  KEY idx_content_block_type (block_type),
  KEY idx_content_block_media_file_id (media_file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE content_relation (
  id BIGINT NOT NULL PRIMARY KEY,
  content_id BIGINT NOT NULL,
  relation_type VARCHAR(32) NOT NULL,
  relation_id BIGINT NOT NULL,
  confidence DECIMAL(5,4) NULL,
  source_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_content_relation_content (content_id),
  KEY idx_relation (relation_type, relation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE comment (
  id BIGINT NOT NULL PRIMARY KEY,
  target_type VARCHAR(32) NOT NULL,
  target_id BIGINT NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  root_id BIGINT NULL,
  reply_to_user_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  content_text VARCHAR(2000) NOT NULL,
  like_count INT NOT NULL DEFAULT 0,
  reply_count INT NOT NULL DEFAULT 0,
  hot_score DECIMAL(12,2) NOT NULL DEFAULT 0,
  is_top TINYINT NOT NULL DEFAULT 0,
  user_rating_score DECIMAL(3,1) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_target_time (target_type, target_id, create_time),
  KEY idx_target_hot (target_type, target_id, hot_score),
  KEY idx_parent_id (parent_id),
  KEY idx_root_id (root_id),
  KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE like_record (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_target (user_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE favorite_record (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_target (user_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE follow_record (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  follow_type VARCHAR(32) NOT NULL,
  target_id BIGINT NOT NULL,
  is_main TINYINT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_follow (user_id, follow_type, target_id),
  KEY idx_target_follow (follow_type, target_id),
  KEY idx_user_type (user_id, follow_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE football_league (
  id BIGINT NOT NULL PRIMARY KEY,
  league_name VARCHAR(128) NOT NULL,
  league_name_en VARCHAR(128) NULL,
  country VARCHAR(64) NULL,
  logo_url VARCHAR(512) NULL,
  season VARCHAR(32) NULL,
  league_type VARCHAR(32) NOT NULL DEFAULT 'LEAGUE',
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_type_sort (league_type, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE football_team (
  id BIGINT NOT NULL PRIMARY KEY,
  team_name VARCHAR(128) NOT NULL,
  team_name_en VARCHAR(128) NULL,
  short_name VARCHAR(64) NULL,
  logo_url VARCHAR(512) NULL,
  country VARCHAR(64) NULL,
  city VARCHAR(64) NULL,
  home_stadium VARCHAR(128) NULL,
  founded_year INT NULL,
  coach_name VARCHAR(128) NULL,
  market_value VARCHAR(64) NULL,
  follower_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_team_name (team_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE football_player (
  id BIGINT NOT NULL PRIMARY KEY,
  player_name VARCHAR(128) NOT NULL,
  player_name_en VARCHAR(128) NULL,
  avatar_url VARCHAR(512) NULL,
  nationality VARCHAR(128) NULL,
  shirt_number INT NULL,
  position VARCHAR(32) NULL,
  birth_date DATE NULL,
  height_cm INT NULL,
  weight_kg INT NULL,
  market_value VARCHAR(64) NULL,
  retired TINYINT NOT NULL DEFAULT 0,
  follower_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE team_player (
  id BIGINT NOT NULL PRIMARY KEY,
  team_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  team_type VARCHAR(32) NOT NULL DEFAULT 'CLUB',
  season VARCHAR(32) NULL,
  shirt_number INT NULL,
  position VARCHAR(32) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_team_season (team_id, season),
  KEY idx_player (player_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE match_info (
  id BIGINT NOT NULL PRIMARY KEY,
  league_id BIGINT NOT NULL,
  season VARCHAR(32) NULL,
  round_name VARCHAR(64) NULL,
  home_team_id BIGINT NOT NULL,
  away_team_id BIGINT NOT NULL,
  home_score INT NULL,
  away_score INT NULL,
  match_time DATETIME NOT NULL,
  venue VARCHAR(128) NULL,
  match_status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
  important_level INT NOT NULL DEFAULT 0,
  has_report TINYINT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_league_time (league_id, match_time),
  KEY idx_home_time (home_team_id, match_time),
  KEY idx_away_time (away_team_id, match_time),
  KEY idx_status_time (match_status, match_time),
  KEY idx_important_time (important_level, match_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE match_event (
  id BIGINT NOT NULL PRIMARY KEY,
  match_id BIGINT NOT NULL,
  team_id BIGINT NULL,
  player_id BIGINT NULL,
  assist_player_id BIGINT NULL,
  event_type VARCHAR(32) NOT NULL,
  minute INT NOT NULL,
  extra_minute INT NULL,
  score_after VARCHAR(32) NULL,
  description VARCHAR(512) NULL,
  has_debate TINYINT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_match_minute (match_id, minute)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE match_report (
  id BIGINT NOT NULL PRIMARY KEY,
  match_id BIGINT NOT NULL,
  content_id BIGINT NOT NULL,
  report_type VARCHAR(32) NOT NULL DEFAULT 'REPORT',
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_match_content (match_id, content_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE admin_operation_log (
  id BIGINT NOT NULL PRIMARY KEY,
  admin_user_id BIGINT NOT NULL,
  operation_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NULL,
  target_id BIGINT NULL,
  operation_desc VARCHAR(512) NULL,
  request_ip VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  is_deleted TINYINT NOT NULL DEFAULT 0,
  extra_json JSON NULL,
  remark VARCHAR(512) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
