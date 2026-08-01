USE south_stand;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS football_team_season_player (
  id BIGINT NOT NULL PRIMARY KEY,
  league_id BIGINT NOT NULL,
  season_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  player_id BIGINT NOT NULL,
  position VARCHAR(32) NOT NULL,
  shirt_number INT NULL,
  squad_role VARCHAR(32) NOT NULL DEFAULT 'FIRST_TEAM',
  captain_flag TINYINT NOT NULL DEFAULT 0,
  loan_flag TINYINT NOT NULL DEFAULT 0,
  loan_from_team_id BIGINT NULL,
  joined_date DATE NULL,
  left_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  source VARCHAR(32) NOT NULL DEFAULT 'DEMO',
  source_record_id VARCHAR(128) NULL,
  source_updated_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_team_season_player (season_id,team_id,player_id),
  KEY idx_roster_team_season (team_id,season_id,status),
  KEY idx_roster_player (player_id,season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS football_team_honor (
  id BIGINT NOT NULL PRIMARY KEY,
  team_id BIGINT NOT NULL,
  league_id BIGINT NULL,
  honor_name VARCHAR(128) NOT NULL,
  honor_type VARCHAR(32) NOT NULL,
  title_count INT NOT NULL DEFAULT 1,
  winning_years VARCHAR(1024) NULL,
  latest_year INT NULL,
  source VARCHAR(32) NOT NULL DEFAULT 'DEMO',
  source_record_id VARCHAR(128) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_team_honor (team_id,honor_name,honor_type),
  KEY idx_team_honor_team (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS football_player_team_history (
  id BIGINT NOT NULL PRIMARY KEY,
  player_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  season_id BIGINT NULL,
  start_date DATE NULL,
  end_date DATE NULL,
  shirt_number INT NULL,
  position VARCHAR(32) NULL,
  appearances INT NOT NULL DEFAULT 0,
  goals INT NOT NULL DEFAULT 0,
  assists INT NOT NULL DEFAULT 0,
  current_flag TINYINT NOT NULL DEFAULT 0,
  loan_flag TINYINT NOT NULL DEFAULT 0,
  source VARCHAR(32) NOT NULL DEFAULT 'DEMO',
  source_record_id VARCHAR(128) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_player_team_history_scope (player_id,team_id,season_id),
  KEY idx_player_history_player (player_id,start_date),
  KEY idx_player_history_team (team_id,start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
