USE south_stand;
SET NAMES utf8mb4;

SELECT 'target_match' check_name,COUNT(*) value FROM match_info WHERE id=15000000000000060 AND league_id=12000000000000004 AND home_team_id=13000000000000012 AND away_team_id=13000000000000011 AND home_score=2 AND away_score=0 AND match_status='FINISHED' AND status='ACTIVE' AND is_deleted=0;

SELECT 'team_roster_counts' check_name,r.team_id,t.team_name,COUNT(*) total,
       SUM(r.position='GOALKEEPER') goalkeepers,SUM(r.position='DEFENDER') defenders,SUM(r.position='MIDFIELDER') midfielders,SUM(r.position='FORWARD') forwards
FROM football_team_season_player r JOIN football_team t ON t.id=r.team_id
WHERE r.season_id=20000000000000008 AND r.team_id IN(13000000000000011,13000000000000012) AND r.status='ACTIVE' AND r.is_deleted=0
GROUP BY r.team_id,t.team_name ORDER BY r.team_id;

SELECT 'roster_under_18' check_name,COUNT(*) value FROM (
  SELECT team_id FROM football_team_season_player WHERE season_id=20000000000000008 AND team_id IN(13000000000000011,13000000000000012) AND status='ACTIVE' AND is_deleted=0 GROUP BY team_id HAVING COUNT(*)<18 OR SUM(position='GOALKEEPER')<2 OR SUM(position='DEFENDER')<5 OR SUM(position='MIDFIELDER')<5 OR SUM(position='FORWARD')<3
) x;

SELECT 'lineup_shape' check_name,team_id,SUM(started_flag=1) starters,SUM(lineup_type='SUBSTITUTE') substitutes,COUNT(*) total
FROM football_match_player_appearance WHERE match_id=15000000000000060 AND status='ACTIVE' AND is_deleted=0 GROUP BY team_id ORDER BY team_id;
SELECT 'lineup_shape_errors' check_name,COUNT(*) value FROM (
  SELECT team_id FROM football_match_player_appearance WHERE match_id=15000000000000060 AND status='ACTIVE' AND is_deleted=0 GROUP BY team_id HAVING SUM(started_flag=1)<>11 OR SUM(lineup_type='SUBSTITUTE') NOT BETWEEN 5 AND 7
) x;
SELECT 'duplicate_match_players' check_name,COUNT(*) value FROM (
  SELECT player_id FROM football_match_player_appearance WHERE match_id=15000000000000060 AND status='ACTIVE' AND is_deleted=0 GROUP BY player_id HAVING COUNT(DISTINCT team_id)>1
) x;
SELECT 'coordinate_errors' check_name,COUNT(*) value FROM football_match_player_appearance WHERE match_id=15000000000000060 AND status='ACTIVE' AND is_deleted=0 AND (field_x IS NULL OR field_y IS NULL OR field_x<0 OR field_x>100 OR field_y<0 OR field_y>100);

SELECT 'event_count' check_name,COUNT(*) value FROM match_event WHERE match_id=15000000000000060 AND status='ACTIVE' AND is_deleted=0;
SELECT 'goal_score_mismatches' check_name,COUNT(*) value FROM (
  SELECT m.home_team_id team_id,m.home_score score,COUNT(e.id) goals FROM match_info m LEFT JOIN match_event e ON e.match_id=m.id AND e.team_id=m.home_team_id AND e.event_type='GOAL' AND e.status='ACTIVE' AND e.is_deleted=0 WHERE m.id=15000000000000060 GROUP BY m.home_team_id,m.home_score
  UNION ALL
  SELECT m.away_team_id, m.away_score,COUNT(e.id) FROM match_info m LEFT JOIN match_event e ON e.match_id=m.id AND e.team_id=m.away_team_id AND e.event_type='GOAL' AND e.status='ACTIVE' AND e.is_deleted=0 WHERE m.id=15000000000000060 GROUP BY m.away_team_id,m.away_score
) x WHERE score<>goals;
SELECT 'event_type_coverage' check_name,event_type,COUNT(*) value FROM match_event WHERE match_id=15000000000000060 AND status='ACTIVE' AND is_deleted=0 GROUP BY event_type ORDER BY event_type;

SELECT 'team_stat_shape' check_name,COUNT(*) value FROM football_match_team_stat WHERE match_id=15000000000000060 AND team_id IN(13000000000000011,13000000000000012) AND is_deleted=0;
SELECT 'team_stat_errors' check_name,COUNT(*) value FROM football_match_team_stat WHERE match_id=15000000000000060 AND is_deleted=0 AND (possession<0 OR possession>100 OR passes<successful_passes OR pass_accuracy<0 OR pass_accuracy>100);
SELECT 'possession_total' check_name,COALESCE(SUM(possession),0) value FROM football_match_team_stat WHERE match_id=15000000000000060 AND is_deleted=0;
SELECT 'player_stat_shape' check_name,COUNT(*) value FROM football_match_player_stat WHERE match_id=15000000000000060 AND is_deleted=0;
SELECT 'player_stat_missing_appeared' check_name,COUNT(*) value FROM football_match_player_appearance a LEFT JOIN football_match_player_stat s ON s.match_id=a.match_id AND s.player_id=a.player_id AND s.is_deleted=0 WHERE a.match_id=15000000000000060 AND a.appeared_flag=1 AND a.status='ACTIVE' AND a.is_deleted=0 AND s.id IS NULL;
SELECT 'ratings_shape' check_name,COUNT(*) value FROM football_user_player_rating WHERE match_id=15000000000000060 AND status='ACTIVE' AND is_deleted=0;

SELECT 'target_standings' check_name,COUNT(*) value FROM football_standing WHERE league_id=12000000000000004 AND season_id=20000000000000008 AND stage_id=21000000000000008 AND team_id IN(13000000000000011,13000000000000012) AND is_deleted=0;
SELECT 'target_team_competition_stats' check_name,COUNT(*) value FROM football_team_competition_stat WHERE league_id=12000000000000004 AND season_id=20000000000000008 AND stage_id=21000000000000008 AND team_id IN(13000000000000011,13000000000000012) AND is_deleted=0;
SELECT 'target_player_competition_stats' check_name,COUNT(*) value FROM football_player_competition_stat WHERE league_id=12000000000000004 AND season_id=20000000000000008 AND stage_id=21000000000000008 AND team_id IN(13000000000000011,13000000000000012) AND is_deleted=0;

SELECT 'non_demo_leakage' check_name,COUNT(*) value FROM (
  SELECT id FROM football_player WHERE id>=36000000000000001 AND id<36100000000000001 AND (remark IS NULL OR remark NOT LIKE 'P1_M2_DEMO_ROSTER:%')
  UNION ALL SELECT id FROM team_player WHERE id>=36100000000000001 AND id<36200000000000001 AND (remark IS NULL OR remark NOT LIKE 'P1_M2_DEMO_ROSTER:%')
  UNION ALL SELECT id FROM football_team_season_player WHERE id>=36200000000000001 AND id<36300000000000001 AND source<>'DEMO'
  UNION ALL SELECT id FROM football_player_team_history WHERE id>=36300000000000001 AND id<36400000000000001 AND source<>'DEMO'
  UNION ALL SELECT id FROM football_player_competition_stat WHERE id>=36400000000000001 AND id<36500000000000001 AND source<>'DEMO'
  UNION ALL SELECT id FROM football_match_lineup WHERE id>=36500000000000001 AND id<36600000000000001 AND source<>'DEMO'
  UNION ALL SELECT id FROM football_match_player_appearance WHERE id>=36600000000000001 AND id<36700000000000001 AND source<>'DEMO'
  UNION ALL SELECT id FROM football_match_team_stat WHERE id>=36700000000000001 AND id<36800000000000001 AND source<>'DEMO'
  UNION ALL SELECT id FROM football_match_player_stat WHERE id>=36800000000000001 AND id<36900000000000001 AND source<>'DEMO'
  UNION ALL SELECT id FROM football_standing WHERE id>=37100000000000001 AND id<37200000000000001 AND source<>'DEMO'
  UNION ALL SELECT id FROM football_team_competition_stat WHERE id>=37200000000000001 AND id<37300000000000001 AND source<>'DEMO'
) x;
