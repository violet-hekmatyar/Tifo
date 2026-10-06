USE south_stand;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS football_season (
  id BIGINT NOT NULL PRIMARY KEY, league_id BIGINT NOT NULL, season_code VARCHAR(32) NOT NULL,
  season_name VARCHAR(64) NOT NULL, start_date DATE NOT NULL, end_date DATE NOT NULL,
  current_flag TINYINT NOT NULL DEFAULT 0, status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  source VARCHAR(32) NOT NULL DEFAULT 'DEMO', source_record_id VARCHAR(128) NULL,
  source_updated_at DATETIME NULL, synced_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_league_season_code (league_id,season_code),
  KEY idx_season_league (league_id), KEY idx_season_current (league_id,current_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS football_competition_stage (
  id BIGINT NOT NULL PRIMARY KEY, league_id BIGINT NOT NULL, season_id BIGINT NOT NULL,
  stage_type VARCHAR(32) NOT NULL, stage_name VARCHAR(64) NOT NULL, group_code VARCHAR(32) NULL,
  sort_order INT NOT NULL DEFAULT 1, status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_stage_league_season (league_id,season_id), KEY idx_stage_group (season_id,group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS football_standing (
  id BIGINT NOT NULL PRIMARY KEY, league_id BIGINT NOT NULL, season_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL DEFAULT 0, group_code VARCHAR(32) NOT NULL DEFAULT '', team_id BIGINT NOT NULL,
  rank_no INT NOT NULL, played INT NOT NULL DEFAULT 0, won INT NOT NULL DEFAULT 0,
  drawn INT NOT NULL DEFAULT 0, lost INT NOT NULL DEFAULT 0, goals_for INT NOT NULL DEFAULT 0,
  goals_against INT NOT NULL DEFAULT 0, goal_difference INT NOT NULL DEFAULT 0, points INT NOT NULL DEFAULT 0,
  deduction_points INT NOT NULL DEFAULT 0, form_text VARCHAR(32) NULL,
  source VARCHAR(32) NOT NULL DEFAULT 'DEMO', source_updated_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_standing_scope_team (league_id,season_id,stage_id,group_code,team_id),
  KEY idx_standing_scope (league_id,season_id,stage_id,group_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS football_player_competition_stat (
  id BIGINT NOT NULL PRIMARY KEY, league_id BIGINT NOT NULL, season_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL DEFAULT 0, player_id BIGINT NOT NULL, team_id BIGINT NOT NULL,
  appearances INT NOT NULL DEFAULT 0, starts INT NOT NULL DEFAULT 0, minutes INT NOT NULL DEFAULT 0,
  goals INT NOT NULL DEFAULT 0, assists INT NOT NULL DEFAULT 0, yellow_cards INT NOT NULL DEFAULT 0,
  red_cards INT NOT NULL DEFAULT 0, shots INT NOT NULL DEFAULT 0, shots_on_target INT NOT NULL DEFAULT 0,
  saves INT NOT NULL DEFAULT 0, rating DECIMAL(4,2) NULL, source VARCHAR(32) NOT NULL DEFAULT 'DEMO',
  source_updated_at DATETIME NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_player_stat_scope (league_id,season_id,stage_id,player_id,team_id),
  KEY idx_player_stat_scope (league_id,season_id,stage_id), KEY idx_player_stat_player (player_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS football_team_competition_stat (
  id BIGINT NOT NULL PRIMARY KEY, league_id BIGINT NOT NULL, season_id BIGINT NOT NULL,
  stage_id BIGINT NOT NULL DEFAULT 0, team_id BIGINT NOT NULL, played INT NOT NULL DEFAULT 0,
  goals_for INT NOT NULL DEFAULT 0, goals_against INT NOT NULL DEFAULT 0, assists INT NOT NULL DEFAULT 0,
  yellow_cards INT NOT NULL DEFAULT 0, red_cards INT NOT NULL DEFAULT 0, shots INT NOT NULL DEFAULT 0,
  shots_on_target INT NOT NULL DEFAULT 0, corners INT NOT NULL DEFAULT 0, fouls INT NOT NULL DEFAULT 0,
  clean_sheets INT NOT NULL DEFAULT 0, avg_rating DECIMAL(4,2) NULL,
  source VARCHAR(32) NOT NULL DEFAULT 'DEMO', source_updated_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_team_stat_scope (league_id,season_id,stage_id,team_id),
  KEY idx_team_stat_scope (league_id,season_id,stage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS t15_add_index_if_missing;
DELIMITER $$
CREATE PROCEDURE t15_add_index_if_missing(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_columns VARCHAR(512), IN p_unique BOOLEAN)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=p_table AND index_name=p_index) THEN
    SET @ddl=CONCAT('ALTER TABLE `',p_table,'` ADD ',IF(p_unique,'UNIQUE ',''),'INDEX `',p_index,'` (',p_columns,')');
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;
-- uk_standing_scope_rank 已废弃：重复名次会导致整批导入失败，约束源数据质量不该由 DB 承担。
-- 由 V020__standings_drop_rank_unique.sql 负责删除，这里只兜底防止被旧的 add-if-missing 复活。
DROP PROCEDURE IF EXISTS t15_drop_index_if_exists;
DELIMITER $$
CREATE PROCEDURE t15_drop_index_if_exists(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=p_table AND index_name=p_index) THEN
    SET @ddl=CONCAT('ALTER TABLE `',p_table,'` DROP INDEX `',p_index,'`');
    PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;
CALL t15_add_index_if_missing('football_season','uk_league_season_code','league_id,season_code',TRUE);
CALL t15_add_index_if_missing('football_season','idx_season_league','league_id',FALSE);
CALL t15_add_index_if_missing('football_season','idx_season_current','league_id,current_flag',FALSE);
CALL t15_add_index_if_missing('football_competition_stage','idx_stage_league_season','league_id,season_id',FALSE);
CALL t15_add_index_if_missing('football_competition_stage','idx_stage_group','season_id,group_code',FALSE);
CALL t15_add_index_if_missing('football_standing','uk_standing_scope_team','league_id,season_id,stage_id,group_code,team_id',TRUE);
CALL t15_add_index_if_missing('football_standing','idx_standing_scope','league_id,season_id,stage_id,group_code',FALSE);
CALL t15_add_index_if_missing('football_player_competition_stat','uk_player_stat_scope','league_id,season_id,stage_id,player_id,team_id',TRUE);
CALL t15_add_index_if_missing('football_player_competition_stat','idx_player_stat_scope','league_id,season_id,stage_id',FALSE);
CALL t15_add_index_if_missing('football_player_competition_stat','idx_player_stat_player','player_id',FALSE);
CALL t15_add_index_if_missing('football_team_competition_stat','uk_team_stat_scope','league_id,season_id,stage_id,team_id',TRUE);
CALL t15_add_index_if_missing('football_team_competition_stat','idx_team_stat_scope','league_id,season_id,stage_id',FALSE);
DROP PROCEDURE t15_add_index_if_missing;
CALL t15_drop_index_if_exists('football_standing','uk_standing_scope_rank');
DROP PROCEDURE t15_drop_index_if_exists;
