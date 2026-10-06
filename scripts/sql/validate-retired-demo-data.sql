USE south_stand;
SET NAMES utf8mb4;

-- 校验 retire-demo-data.sql 的效果：
--   1) 演示段在「App 可见」的过滤条件下必须为 0 行；
--   2) 真实数据（football-data.org 全局 ID）必须≥预期，且 App 看到的联赛列表只有真实赛事；
--   3) 同步脚本写入的榜单/比赛依然能按 App 的查询路径查到。

SELECT '--- 1. 演示数据在 App 可见条件下剩余行数（都应为 0）---' AS section;
SELECT 'sys_user' AS tbl, COUNT(*) AS remaining FROM sys_user
  WHERE is_deleted = 0 AND id >= 11000000000000000 AND id < 12000000000000000
UNION ALL SELECT 'football_league', COUNT(*) FROM football_league
  WHERE is_deleted = 0 AND id >= 12000000000000000 AND id < 13000000000000000
UNION ALL SELECT 'football_season', COUNT(*) FROM football_season
  WHERE is_deleted = 0 AND id >= 20000000000000000 AND id < 21000000000000000
UNION ALL SELECT 'football_competition_stage', COUNT(*) FROM football_competition_stage
  WHERE is_deleted = 0 AND id >= 21000000000000000 AND id < 22000000000000000
UNION ALL SELECT 'football_team', COUNT(*) FROM football_team
  WHERE is_deleted = 0 AND id >= 13000000000000000 AND id < 14000000000000000
UNION ALL SELECT 'football_player', COUNT(*) FROM football_player
  WHERE is_deleted = 0 AND id >= 14000000000000000 AND id < 15000000000000000
UNION ALL SELECT 'team_player', COUNT(*) FROM team_player
  WHERE is_deleted = 0 AND id >= 14000000000000000 AND id < 15000000000000000
UNION ALL SELECT 'match_info', COUNT(*) FROM match_info
  WHERE is_deleted = 0 AND id >= 15000000000000000 AND id < 16000000000000000
UNION ALL SELECT 'match_event', COUNT(*) FROM match_event
  WHERE is_deleted = 0 AND id >= 15000000000000000 AND id < 16000000000000000
UNION ALL SELECT 'football_standing', COUNT(*) FROM football_standing
  WHERE is_deleted = 0 AND id >= 22000000000000000 AND id < 23000000000000000
UNION ALL SELECT 'football_player_competition_stat', COUNT(*) FROM football_player_competition_stat
  WHERE is_deleted = 0 AND id >= 23000000000000000 AND id < 24000000000000000
UNION ALL SELECT 'football_team_competition_stat', COUNT(*) FROM football_team_competition_stat
  WHERE is_deleted = 0 AND id >= 24000000000000000 AND id < 25000000000000000;

SELECT '--- 2. App 的联赛列表查询（GET /api/football/leagues 走的条件）---' AS section;
SELECT id, league_name, league_name_en, season, league_type, sort_order
FROM football_league
WHERE status = 'ACTIVE' AND is_deleted = 0
ORDER BY sort_order, id;

SELECT '--- 2b. seed.sql 老 smoke 数据是否也退役（都应为 0）---' AS section;
SELECT 'football_league 10001-10003' AS item, COUNT(*) AS remaining FROM football_league
  WHERE is_deleted = 0 AND id IN (10001, 10002, 10003)
UNION ALL SELECT 'football_team 30001-30006', COUNT(*) FROM football_team
  WHERE is_deleted = 0 AND id IN (30001, 30002, 30003, 30004, 30005, 30006)
UNION ALL SELECT 'football_player 40001-40010', COUNT(*) FROM football_player
  WHERE is_deleted = 0 AND id IN (40001, 40002, 40003, 40004, 40005, 40006, 40007, 40008, 40009, 40010)
UNION ALL SELECT 'team_player 41001-41010', COUNT(*) FROM team_player
  WHERE is_deleted = 0 AND id IN (41001, 41002, 41003, 41004, 41005, 41006, 41007, 41008, 41009, 41010)
UNION ALL SELECT 'match_info 50001-50006', COUNT(*) FROM match_info
  WHERE is_deleted = 0 AND id IN (50001, 50002, 50003, 50004, 50005, 50006)
UNION ALL SELECT 'match_event 51001-51007', COUNT(*) FROM match_event
  WHERE is_deleted = 0 AND id IN (51001, 51002, 51003, 51004, 51005, 51006, 51007);

SELECT '--- 3. 「重要」tab 查询（important_level > 0）---' AS section;
SELECT COUNT(*) AS important_matches FROM match_info
WHERE status = 'ACTIVE' AND is_deleted = 0 AND important_level > 0;
SELECT important_level, COUNT(*) AS cnt FROM match_info
WHERE status = 'ACTIVE' AND is_deleted = 0 AND important_level > 0
GROUP BY important_level ORDER BY important_level DESC;

SELECT '--- 4. 榜单仍可查（英超 2026/27 积分榜前 5）---' AS section;
SELECT s.rank_no, t.team_name, s.played, s.points, s.source
FROM football_standing s
JOIN football_season se ON se.id = s.season_id
LEFT JOIN football_team t ON t.id = s.team_id
WHERE s.league_id = 2021 AND se.season_code = '2026' AND s.is_deleted = 0
ORDER BY s.rank_no LIMIT 5;

SELECT '--- 5. 球队榜（由积分榜派生）---' AS section;
SELECT ts.team_id, t.team_name, ts.played, ts.goals_for, ts.goals_against
FROM football_team_competition_stat ts
JOIN football_season se ON se.id = ts.season_id
LEFT JOIN football_team t ON t.id = ts.team_id
WHERE ts.league_id = 2021 AND se.season_code = '2026' AND ts.is_deleted = 0
ORDER BY ts.goals_for DESC LIMIT 5;

SELECT '--- 6. 榜单与阶段一致性（不一致必须为 0）---' AS section;
SELECT COUNT(*) AS orphan_standing
FROM football_standing s
LEFT JOIN football_competition_stage g ON g.id = s.stage_id AND g.season_id = s.season_id
WHERE s.is_deleted = 0 AND g.id IS NULL;

SELECT '--- 7. 已删除的重复名次唯一键（V020 应已删除）---' AS section;
SELECT COUNT(*) AS rank_unique_index_present
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'football_standing'
  AND index_name = 'uk_standing_scope_rank';
