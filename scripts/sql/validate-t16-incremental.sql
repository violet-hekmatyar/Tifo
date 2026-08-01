USE south_stand;
SET NAMES utf8mb4;
SET collation_connection='utf8mb4_unicode_ci';

SELECT check_name,anomaly_count FROM (
  SELECT 'roster_missing_relation' check_name,COUNT(*) anomaly_count FROM football_team_season_player r LEFT JOIN football_season s ON s.id=r.season_id LEFT JOIN football_team t ON t.id=r.team_id LEFT JOIN football_player p ON p.id=r.player_id LEFT JOIN team_player tp ON tp.team_id=r.team_id AND tp.player_id=r.player_id AND tp.status='ACTIVE' AND tp.is_deleted=0 WHERE s.id IS NULL OR s.league_id<>r.league_id OR t.id IS NULL OR p.id IS NULL OR tp.id IS NULL
  UNION ALL SELECT 'roster_duplicate_relation',COUNT(*) FROM (SELECT season_id,team_id,player_id FROM football_team_season_player WHERE is_deleted=0 GROUP BY season_id,team_id,player_id HAVING COUNT(*)>1)x
  UNION ALL SELECT 'roster_duplicate_shirt',COUNT(*) FROM (SELECT season_id,team_id,shirt_number FROM football_team_season_player WHERE status='ACTIVE' AND is_deleted=0 AND shirt_number IS NOT NULL GROUP BY season_id,team_id,shirt_number HAVING COUNT(*)>1)x
  UNION ALL SELECT 'roster_multiple_captains',COUNT(*) FROM (SELECT season_id,team_id FROM football_team_season_player WHERE status='ACTIVE' AND is_deleted=0 AND captain_flag=1 GROUP BY season_id,team_id HAVING COUNT(*)>1)x
  UNION ALL SELECT 'roster_invalid_loan',COUNT(*) FROM football_team_season_player WHERE loan_flag=1 AND loan_from_team_id IS NULL
  UNION ALL SELECT 'roster_invalid_dates',COUNT(*) FROM football_team_season_player WHERE joined_date>left_date
  UNION ALL SELECT 'roster_stat_mismatch',COUNT(*) FROM football_team_season_player r LEFT JOIN football_player_competition_stat ps ON ps.league_id=r.league_id AND ps.season_id=r.season_id AND ps.team_id=r.team_id AND ps.player_id=r.player_id AND ps.is_deleted=0 WHERE r.source='DEMO' AND ps.id IS NULL
  UNION ALL SELECT 'honor_missing_team',COUNT(*) FROM football_team_honor h LEFT JOIN football_team t ON t.id=h.team_id WHERE t.id IS NULL
  UNION ALL SELECT 'honor_invalid_count',COUNT(*) FROM football_team_honor WHERE title_count<1
  UNION ALL SELECT 'honor_latest_not_listed',COUNT(*) FROM football_team_honor WHERE latest_year IS NOT NULL AND FIND_IN_SET(CAST(latest_year AS CHAR),REPLACE(winning_years,' ',''))=0
  UNION ALL SELECT 'history_missing_relation',COUNT(*) FROM football_player_team_history h LEFT JOIN football_player p ON p.id=h.player_id LEFT JOIN football_team t ON t.id=h.team_id LEFT JOIN team_player tp ON tp.player_id=h.player_id AND tp.team_id=h.team_id AND tp.status='ACTIVE' AND tp.is_deleted=0 WHERE p.id IS NULL OR t.id IS NULL OR (h.current_flag=1 AND tp.id IS NULL)
  UNION ALL SELECT 'history_invalid_dates',COUNT(*) FROM football_player_team_history WHERE start_date>end_date
  UNION ALL SELECT 'history_multiple_current',COUNT(*) FROM (SELECT player_id FROM football_player_team_history WHERE current_flag=1 AND is_deleted=0 GROUP BY player_id HAVING COUNT(*)>1)x
  UNION ALL SELECT 'history_stats_exceed_t15',COUNT(*) FROM football_player_team_history h LEFT JOIN (SELECT player_id,team_id,season_id,SUM(appearances) appearances,SUM(goals) goals,SUM(assists) assists FROM football_player_competition_stat WHERE is_deleted=0 GROUP BY player_id,team_id,season_id) ps ON ps.player_id=h.player_id AND ps.team_id=h.team_id AND ps.season_id=h.season_id WHERE h.appearances>COALESCE(ps.appearances,0) OR h.goals>COALESCE(ps.goals,0) OR h.assists>COALESCE(ps.assists,0)
  UNION ALL SELECT 'roster_teams_below_20',IF(COUNT(*)>=20,0,1) FROM (SELECT DISTINCT team_id FROM football_team_season_player WHERE status='ACTIVE' AND is_deleted=0)x
  UNION ALL SELECT 'history_players_below_120',IF(COUNT(*)>=120,0,1) FROM (SELECT DISTINCT player_id FROM football_player_team_history WHERE current_flag=1 AND is_deleted=0)x
) checks ORDER BY check_name;
