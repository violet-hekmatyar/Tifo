USE south_stand;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS football_match_lineup (
  id BIGINT NOT NULL PRIMARY KEY, match_id BIGINT NOT NULL, team_id BIGINT NOT NULL, formation VARCHAR(32) NULL, coach_name VARCHAR(128) NULL,
  confirmed_flag TINYINT NOT NULL DEFAULT 1, source VARCHAR(32) NOT NULL DEFAULT 'DEMO', source_record_id VARCHAR(128) NULL, source_updated_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_match_lineup_team (match_id,team_id), KEY idx_match_lineup_match (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS football_match_player_appearance (
  id BIGINT NOT NULL PRIMARY KEY, match_id BIGINT NOT NULL, team_id BIGINT NOT NULL, player_id BIGINT NOT NULL, lineup_type VARCHAR(32) NOT NULL, position VARCHAR(32) NOT NULL,
  shirt_number INT NULL, captain_flag TINYINT NOT NULL DEFAULT 0, started_flag TINYINT NOT NULL DEFAULT 0, appeared_flag TINYINT NOT NULL DEFAULT 0,
  start_minute INT NULL, end_minute INT NULL, substituted_in_minute INT NULL, substituted_out_minute INT NULL, field_x DECIMAL(5,2) NULL, field_y DECIMAL(5,2) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', source VARCHAR(32) NOT NULL DEFAULT 'DEMO', source_record_id VARCHAR(128) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_match_player (match_id,player_id), KEY idx_appearance_match_team (match_id,team_id), KEY idx_appearance_player (player_id,match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS football_match_team_stat (
  id BIGINT NOT NULL PRIMARY KEY, match_id BIGINT NOT NULL, team_id BIGINT NOT NULL, possession DECIMAL(5,2) NULL, shots INT NOT NULL DEFAULT 0,
  shots_on_target INT NOT NULL DEFAULT 0, corners INT NOT NULL DEFAULT 0, fouls INT NOT NULL DEFAULT 0, offsides INT NOT NULL DEFAULT 0,
  yellow_cards INT NOT NULL DEFAULT 0, red_cards INT NOT NULL DEFAULT 0, passes INT NOT NULL DEFAULT 0, successful_passes INT NOT NULL DEFAULT 0,
  pass_accuracy DECIMAL(5,2) NULL, saves INT NOT NULL DEFAULT 0, expected_goals DECIMAL(5,2) NULL, source VARCHAR(32) NOT NULL DEFAULT 'DEMO', source_updated_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_match_team_stat (match_id,team_id), KEY idx_match_team_stat_match (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS football_match_player_stat (
  id BIGINT NOT NULL PRIMARY KEY, match_id BIGINT NOT NULL, team_id BIGINT NOT NULL, player_id BIGINT NOT NULL, minutes INT NOT NULL DEFAULT 0,
  goals INT NOT NULL DEFAULT 0, assists INT NOT NULL DEFAULT 0, shots INT NOT NULL DEFAULT 0, shots_on_target INT NOT NULL DEFAULT 0,
  passes INT NOT NULL DEFAULT 0, successful_passes INT NOT NULL DEFAULT 0, key_passes INT NOT NULL DEFAULT 0, tackles INT NOT NULL DEFAULT 0,
  interceptions INT NOT NULL DEFAULT 0, saves INT NOT NULL DEFAULT 0, yellow_cards INT NOT NULL DEFAULT 0, red_cards INT NOT NULL DEFAULT 0,
  official_rating DECIMAL(4,2) NULL, source VARCHAR(32) NOT NULL DEFAULT 'DEMO', source_updated_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_match_player_stat (match_id,player_id), KEY idx_match_player_stat_match_team (match_id,team_id), KEY idx_match_player_stat_player (player_id,match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS football_user_player_rating (
  id BIGINT NOT NULL PRIMARY KEY, match_id BIGINT NOT NULL, player_id BIGINT NOT NULL, user_id BIGINT NOT NULL, rating DECIMAL(3,1) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_user_match_player (user_id,match_id,player_id), KEY idx_rating_match_player (match_id,player_id,status), KEY idx_rating_user (user_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
