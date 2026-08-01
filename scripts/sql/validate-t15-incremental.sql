USE south_stand;
SET NAMES utf8mb4;

SELECT check_name,anomaly_count FROM (
  SELECT 'season_invalid_dates' check_name,COUNT(*) anomaly_count FROM football_season WHERE start_date>=end_date
  UNION ALL SELECT 'season_multiple_current',COUNT(*) FROM (SELECT league_id FROM football_season WHERE current_flag=1 AND status='ACTIVE' AND is_deleted=0 GROUP BY league_id HAVING COUNT(*)>1) x
  UNION ALL SELECT 'season_duplicate_code',COUNT(*) FROM (SELECT league_id,season_code FROM football_season GROUP BY league_id,season_code HAVING COUNT(*)>1) x
  UNION ALL SELECT 'stage_missing_scope',COUNT(*) FROM football_competition_stage st LEFT JOIN football_season s ON s.id=st.season_id WHERE s.id IS NULL OR s.league_id<>st.league_id
  UNION ALL SELECT 'standing_played_formula',COUNT(*) FROM football_standing WHERE played<>won+drawn+lost
  UNION ALL SELECT 'standing_goal_difference_formula',COUNT(*) FROM football_standing WHERE goal_difference<>goals_for-goals_against
  UNION ALL SELECT 'standing_points_formula',COUNT(*) FROM football_standing WHERE points<>won*3+drawn-deduction_points
  UNION ALL SELECT 'standing_duplicate_team',COUNT(*) FROM (SELECT league_id,season_id,stage_id,group_code,team_id FROM football_standing GROUP BY league_id,season_id,stage_id,group_code,team_id HAVING COUNT(*)>1) x
  UNION ALL SELECT 'standing_duplicate_rank',COUNT(*) FROM (SELECT league_id,season_id,stage_id,group_code,rank_no FROM football_standing GROUP BY league_id,season_id,stage_id,group_code,rank_no HAVING COUNT(*)>1) x
  UNION ALL SELECT 'player_stat_missing_scope',COUNT(*) FROM football_player_competition_stat ps LEFT JOIN football_season s ON s.id=ps.season_id LEFT JOIN team_player tp ON tp.player_id=ps.player_id AND tp.team_id=ps.team_id AND tp.status='ACTIVE' AND tp.is_deleted=0 WHERE s.id IS NULL OR s.league_id<>ps.league_id OR tp.id IS NULL
  UNION ALL SELECT 'player_stat_invalid_totals',COUNT(*) FROM football_player_competition_stat WHERE appearances<starts OR shots<shots_on_target OR goals>shots_on_target OR LEAST(appearances,starts,minutes,goals,assists,yellow_cards,red_cards,shots,shots_on_target,saves)<0
  UNION ALL SELECT 'team_stat_missing_scope',COUNT(*) FROM football_team_competition_stat ts LEFT JOIN football_season s ON s.id=ts.season_id WHERE s.id IS NULL OR s.league_id<>ts.league_id
  UNION ALL SELECT 'team_stat_invalid_totals',COUNT(*) FROM football_team_competition_stat WHERE shots<shots_on_target OR assists>goals_for OR LEAST(played,goals_for,goals_against,assists,yellow_cards,red_cards,shots,shots_on_target,corners,fouls,clean_sheets)<0
  UNION ALL SELECT 't15_seasons_missing',IF(COUNT(*)>0,0,1) FROM football_season
  UNION ALL SELECT 't15_standings_missing',IF(COUNT(*)>0,0,1) FROM football_standing
  UNION ALL SELECT 't15_player_stats_missing',IF(COUNT(*)>0,0,1) FROM football_player_competition_stat
  UNION ALL SELECT 't15_team_stats_missing',IF(COUNT(*)>0,0,1) FROM football_team_competition_stat
) checks ORDER BY check_name;
