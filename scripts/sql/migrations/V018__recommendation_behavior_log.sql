USE south_stand;
SET NAMES utf8mb4;

-- T18 推荐行为日志：只做增量建表，不修改或清理任何已有业务数据。
CREATE TABLE IF NOT EXISTS user_behavior_log (
  id BIGINT NOT NULL PRIMARY KEY,
  client_event_id VARCHAR(64) NOT NULL,
  user_id BIGINT NULL,
  session_id VARCHAR(64) NULL,
  behavior_type VARCHAR(32) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id BIGINT NOT NULL,
  scene VARCHAR(32) NULL,
  algorithm_version VARCHAR(32) NULL,
  model_version VARCHAR(64) NULL,
  experiment_id VARCHAR(64) NULL,
  experiment_bucket VARCHAR(32) NULL,
  request_id VARCHAR(64) NULL,
  impression_id VARCHAR(96) NULL,
  position INT NULL,
  dwell_ms BIGINT NULL,
  event_time DATETIME(3) NOT NULL,
  extra_json TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
  create_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  update_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_behavior_client_event (client_event_id),
  KEY idx_behavior_user_event_time (user_id,event_time),
  KEY idx_behavior_experiment_time (experiment_id,experiment_bucket,event_time),
  KEY idx_behavior_request_position (request_id,position),
  KEY idx_behavior_impression_type (impression_id,behavior_type),
  KEY idx_behavior_target_event_time (target_type,target_id,behavior_type,event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

