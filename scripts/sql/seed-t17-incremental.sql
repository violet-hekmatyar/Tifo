USE south_stand;
SET NAMES utf8mb4;
SET collation_connection='utf8mb4_unicode_ci';

DROP PROCEDURE IF EXISTS t17_assert_safe;
DELIMITER $$
CREATE PROCEDURE t17_assert_safe()
BEGIN
  IF EXISTS (SELECT 1 FROM football_match_lineup WHERE id>=28000000000000001 AND id<29000000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_match_player_appearance WHERE id>=29000000000000001 AND id<30000000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_match_team_stat WHERE id>=30000000000000001 AND id<31000000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_match_player_stat WHERE id>=31000000000000001 AND id<32000000000000001 AND source<>'DEMO') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='T17 conflict: reserved ID range contains non-DEMO data';
  END IF;
END$$
DELIMITER ;
CALL t17_assert_safe();
DROP PROCEDURE t17_assert_safe;

START TRANSACTION;
DROP TEMPORARY TABLE IF EXISTS t17_safe_matches;
CREATE TEMPORARY TABLE t17_safe_matches AS
SELECT m.id match_id,m.league_id,s.id season_id,m.home_team_id,m.away_team_id,m.home_score,m.away_score,m.match_time
FROM match_info m JOIN football_season s ON s.league_id=m.league_id AND s.current_flag=1 AND s.status='ACTIVE' AND s.is_deleted=0
JOIN football_team h ON h.id=m.home_team_id AND h.status='ACTIVE' AND h.is_deleted=0 JOIN football_team a ON a.id=m.away_team_id AND a.status='ACTIVE' AND a.is_deleted=0
WHERE m.match_status='FINISHED' AND m.status='ACTIVE' AND m.is_deleted=0 AND m.home_score>=0 AND m.away_score>=0
 AND m.home_score=(SELECT COUNT(*) FROM match_event e WHERE e.match_id=m.id AND e.team_id=m.home_team_id AND e.event_type='GOAL' AND e.status='ACTIVE' AND e.is_deleted=0)
 AND m.away_score=(SELECT COUNT(*) FROM match_event e WHERE e.match_id=m.id AND e.team_id=m.away_team_id AND e.event_type='GOAL' AND e.status='ACTIVE' AND e.is_deleted=0)
 AND NOT EXISTS(SELECT 1 FROM match_event e LEFT JOIN football_team_season_player r ON r.season_id=s.id AND r.team_id=e.team_id AND r.player_id=e.player_id AND r.status='ACTIVE' AND r.is_deleted=0 WHERE e.match_id=m.id AND e.status='ACTIVE' AND e.is_deleted=0 AND(e.team_id IS NULL OR e.team_id NOT IN(m.home_team_id,m.away_team_id) OR e.player_id IS NULL OR r.id IS NULL))
 AND NOT EXISTS(SELECT 1 FROM match_event e LEFT JOIN football_team_season_player r ON r.season_id=s.id AND r.team_id=e.team_id AND r.player_id=e.assist_player_id AND r.status='ACTIVE' AND r.is_deleted=0 WHERE e.match_id=m.id AND e.status='ACTIVE' AND e.is_deleted=0 AND e.assist_player_id IS NOT NULL AND r.id IS NULL)
 AND(SELECT COUNT(*) FROM football_team_season_player r WHERE r.season_id=s.id AND r.team_id=m.home_team_id AND r.status='ACTIVE' AND r.is_deleted=0)>=18
 AND(SELECT COUNT(*) FROM football_team_season_player r WHERE r.season_id=s.id AND r.team_id=m.away_team_id AND r.status='ACTIVE' AND r.is_deleted=0)>=18;

DROP TEMPORARY TABLE IF EXISTS t17_target_leagues;
CREATE TEMPORARY TABLE t17_target_leagues AS SELECT league_id FROM t17_safe_matches GROUP BY league_id HAVING COUNT(*)>=8 ORDER BY COUNT(*) DESC,league_id LIMIT 3;
DROP TEMPORARY TABLE IF EXISTS t17_candidates;
CREATE TEMPORARY TABLE t17_candidates AS SELECT s.* FROM t17_safe_matches s JOIN t17_target_leagues l ON l.league_id=s.league_id ORDER BY s.league_id,s.match_time,s.match_id LIMIT 20;

SET @next_id=(SELECT GREATEST(28000000001000001,COALESCE(MAX(id)+1,28000000001000001)) FROM football_match_lineup WHERE id>=28000000000000001 AND id<29000000000000001);
INSERT INTO football_match_lineup(id,match_id,team_id,formation,coach_name,confirmed_flag,source,source_record_id,is_deleted)
SELECT @next_id+ROW_NUMBER()OVER(ORDER BY x.match_id,x.team_id)-1,x.match_id,x.team_id,'4-3-3',t.coach_name,1,'DEMO',CONCAT('t17-lineup-',x.match_id,'-',x.team_id),0
FROM(SELECT c.match_id,IF(side.n=0,c.home_team_id,c.away_team_id)team_id FROM t17_candidates c CROSS JOIN(SELECT 0 n UNION ALL SELECT 1)side)x JOIN football_team t ON t.id=x.team_id
WHERE NOT EXISTS(SELECT 1 FROM football_match_lineup l WHERE l.match_id=x.match_id AND l.team_id=x.team_id);

DROP TEMPORARY TABLE IF EXISTS t17_roster_events;
CREATE TEMPORARY TABLE t17_roster_events AS
SELECT c.match_id,r.team_id,r.player_id,r.position,r.shirt_number,r.captain_flag,
 IF(EXISTS(SELECT 1 FROM match_event e WHERE e.match_id=c.match_id AND e.team_id=r.team_id AND(e.player_id=r.player_id OR e.assist_player_id=r.player_id)AND e.status='ACTIVE' AND e.is_deleted=0),1,0)event_flag
FROM t17_candidates c JOIN football_team_season_player r ON r.season_id=c.season_id AND r.team_id IN(c.home_team_id,c.away_team_id)AND r.status='ACTIVE' AND r.is_deleted=0;
DROP TEMPORARY TABLE IF EXISTS t17_roster_positioned;
CREATE TEMPORARY TABLE t17_roster_positioned AS
SELECT p.*,ROW_NUMBER()OVER(PARTITION BY p.match_id,p.team_id,p.position ORDER BY p.event_flag DESC,p.captain_flag DESC,COALESCE(p.shirt_number,999),p.player_id)pos_no FROM t17_roster_events p;
DROP TEMPORARY TABLE IF EXISTS t17_starter_ranked;
CREATE TEMPORARY TABLE t17_starter_ranked AS
SELECT p.*,ROW_NUMBER()OVER(PARTITION BY p.match_id,p.team_id ORDER BY FIELD(p.position,'GOALKEEPER','DEFENDER','MIDFIELDER','FORWARD'),p.pos_no,p.player_id)starter_rank FROM t17_roster_positioned p
WHERE p.pos_no<=CASE p.position WHEN 'GOALKEEPER' THEN 1 WHEN 'DEFENDER' THEN 4 ELSE 3 END;

SET @next_id=(SELECT GREATEST(29000000001000001,COALESCE(MAX(id)+1,29000000001000001)) FROM football_match_player_appearance WHERE id>=29000000000000001 AND id<30000000000000001);
INSERT INTO football_match_player_appearance(id,match_id,team_id,player_id,lineup_type,position,shirt_number,captain_flag,started_flag,appeared_flag,start_minute,end_minute,substituted_out_minute,field_x,field_y,status,source,source_record_id,is_deleted)
SELECT @next_id+ROW_NUMBER()OVER(ORDER BY p.match_id,p.team_id,p.starter_rank)-1,p.match_id,p.team_id,p.player_id,'STARTER',p.position,p.shirt_number,p.captain_flag,1,1,0,
 COALESCE((SELECT MIN(e.minute)FROM match_event e WHERE e.match_id=p.match_id AND e.team_id=p.team_id AND e.player_id=p.player_id AND e.event_type='SUBSTITUTION' AND e.status='ACTIVE' AND e.is_deleted=0),90),
 (SELECT MIN(e.minute)FROM match_event e WHERE e.match_id=p.match_id AND e.team_id=p.team_id AND e.player_id=p.player_id AND e.event_type='SUBSTITUTION' AND e.status='ACTIVE' AND e.is_deleted=0),
 MOD(p.starter_rank-1,4)*25+12.5,FLOOR((p.starter_rank-1)/4)*32+15,'ACTIVE','DEMO',CONCAT('t17-appearance-',p.match_id,'-',p.player_id),0
FROM t17_starter_ranked p WHERE NOT EXISTS(SELECT 1 FROM football_match_player_appearance a WHERE a.match_id=p.match_id AND a.player_id=p.player_id);

SET @next_id=(SELECT GREATEST(29000000001000001,COALESCE(MAX(id)+1,29000000001000001)) FROM football_match_player_appearance WHERE id>=29000000000000001 AND id<30000000000000001);
INSERT INTO football_match_player_appearance(id,match_id,team_id,player_id,lineup_type,position,shirt_number,captain_flag,started_flag,appeared_flag,status,source,source_record_id,is_deleted)
SELECT @next_id+ROW_NUMBER()OVER(ORDER BY b.match_id,b.team_id,b.bench_rank)-1,b.match_id,b.team_id,b.player_id,'BENCH',b.position,b.shirt_number,0,0,0,'ACTIVE','DEMO',CONCAT('t17-appearance-',b.match_id,'-',b.player_id),0
FROM(SELECT p.*,ROW_NUMBER()OVER(PARTITION BY p.match_id,p.team_id ORDER BY FIELD(p.position,'GOALKEEPER','DEFENDER','MIDFIELDER','FORWARD'),p.pos_no,p.player_id)bench_rank FROM t17_roster_positioned p WHERE p.pos_no>CASE p.position WHEN 'GOALKEEPER' THEN 1 WHEN 'DEFENDER' THEN 4 ELSE 3 END)b
WHERE b.bench_rank<=5 AND NOT EXISTS(SELECT 1 FROM football_match_player_appearance a WHERE a.match_id=b.match_id AND a.player_id=b.player_id);

SET @next_id=(SELECT GREATEST(31000000001000001,COALESCE(MAX(id)+1,31000000001000001)) FROM football_match_player_stat WHERE id>=31000000000000001 AND id<32000000000000001);
INSERT INTO football_match_player_stat(id,match_id,team_id,player_id,minutes,goals,assists,shots,shots_on_target,passes,successful_passes,key_passes,tackles,interceptions,saves,yellow_cards,red_cards,official_rating,source,is_deleted)
SELECT @next_id+ROW_NUMBER()OVER(ORDER BY a.match_id,a.team_id,a.player_id)-1,a.match_id,a.team_id,a.player_id,a.end_minute-a.start_minute,
 SUM(e.event_type='GOAL' AND e.player_id=a.player_id),SUM(e.event_type='GOAL' AND e.assist_player_id=a.player_id),
 SUM(e.event_type='GOAL' AND e.player_id=a.player_id)+MOD(a.player_id,2)+1,SUM(e.event_type='GOAL' AND e.player_id=a.player_id)+IF(MOD(a.player_id,4)=0,1,0),
 20+MOD(a.player_id,16),FLOOR((20+MOD(a.player_id,16))*0.80),SUM(e.event_type='GOAL' AND e.assist_player_id=a.player_id),MOD(a.player_id,4),MOD(a.player_id,3),IF(a.position='GOALKEEPER',2+MOD(a.player_id,2),0),
 SUM(e.event_type='YELLOW_CARD' AND e.player_id=a.player_id),SUM(e.event_type='RED_CARD' AND e.player_id=a.player_id),
 LEAST(9.50,GREATEST(5.00,6.50+SUM(e.event_type='GOAL' AND e.player_id=a.player_id)+SUM(e.event_type='GOAL' AND e.assist_player_id=a.player_id)*0.5-SUM(e.event_type='RED_CARD' AND e.player_id=a.player_id)*1.5)),'DEMO',0
FROM football_match_player_appearance a JOIN t17_candidates c ON c.match_id=a.match_id LEFT JOIN match_event e ON e.match_id=a.match_id AND e.status='ACTIVE' AND e.is_deleted=0
WHERE a.appeared_flag=1 AND a.status='ACTIVE' AND a.is_deleted=0 AND NOT EXISTS(SELECT 1 FROM football_match_player_stat s WHERE s.match_id=a.match_id AND s.player_id=a.player_id)
GROUP BY a.match_id,a.team_id,a.player_id,a.end_minute,a.start_minute,a.position;

SET @next_id=(SELECT GREATEST(30000000001000001,COALESCE(MAX(id)+1,30000000001000001)) FROM football_match_team_stat WHERE id>=30000000000000001 AND id<31000000000000001);
INSERT INTO football_match_team_stat(id,match_id,team_id,possession,shots,shots_on_target,corners,fouls,offsides,yellow_cards,red_cards,passes,successful_passes,pass_accuracy,saves,expected_goals,source,is_deleted)
SELECT @next_id+ROW_NUMBER()OVER(ORDER BY p.match_id,p.team_id)-1,p.match_id,p.team_id,IF(p.team_id=c.home_team_id,49+MOD(c.match_id,7),51-MOD(c.match_id,7)),SUM(p.shots),SUM(p.shots_on_target),3+MOD(p.team_id,5),8+MOD(p.team_id,7),MOD(p.team_id,4),SUM(p.yellow_cards),SUM(p.red_cards),SUM(p.passes),SUM(p.successful_passes),ROUND(SUM(p.successful_passes)*100.0/SUM(p.passes),2),SUM(p.saves),ROUND(SUM(p.goals)*0.75+SUM(p.shots_on_target)*0.12,2),'DEMO',0
FROM football_match_player_stat p JOIN t17_candidates c ON c.match_id=p.match_id WHERE p.is_deleted=0 GROUP BY p.match_id,p.team_id,c.home_team_id,c.match_id
HAVING NOT EXISTS(SELECT 1 FROM football_match_team_stat t WHERE t.match_id=p.match_id AND t.team_id=p.team_id);

DROP TEMPORARY TABLE IF EXISTS t17_player_totals;
CREATE TEMPORARY TABLE t17_player_totals AS SELECT c.league_id,c.season_id,p.team_id,p.player_id,COUNT(*)appearances,SUM(a.started_flag)starts,SUM(p.minutes)minutes,SUM(p.goals)goals,SUM(p.assists)assists,SUM(p.yellow_cards)yellow_cards,SUM(p.red_cards)red_cards,SUM(p.shots)shots,SUM(p.shots_on_target)shots_on_target,SUM(p.saves)saves FROM football_match_player_stat p JOIN t17_candidates c ON c.match_id=p.match_id JOIN football_match_player_appearance a ON a.match_id=p.match_id AND a.player_id=p.player_id WHERE p.is_deleted=0 GROUP BY c.league_id,c.season_id,p.team_id,p.player_id;
UPDATE football_player_competition_stat ps JOIN t17_player_totals x ON x.league_id=ps.league_id AND x.season_id=ps.season_id AND x.team_id=ps.team_id AND x.player_id=ps.player_id SET ps.appearances=GREATEST(ps.appearances,x.appearances),ps.starts=GREATEST(ps.starts,x.starts),ps.minutes=GREATEST(ps.minutes,x.minutes),ps.goals=GREATEST(ps.goals,x.goals),ps.assists=GREATEST(ps.assists,x.assists),ps.yellow_cards=GREATEST(ps.yellow_cards,x.yellow_cards),ps.red_cards=GREATEST(ps.red_cards,x.red_cards),ps.shots=GREATEST(ps.shots,x.shots),ps.shots_on_target=GREATEST(ps.shots_on_target,x.shots_on_target),ps.saves=GREATEST(ps.saves,x.saves) WHERE ps.source='DEMO' AND ps.is_deleted=0;

DROP TEMPORARY TABLE IF EXISTS t17_team_totals;
CREATE TEMPORARY TABLE t17_team_totals AS SELECT c.league_id,c.season_id,t.team_id,SUM(t.shots)shots,SUM(t.shots_on_target)shots_on_target,SUM(t.yellow_cards)yellow_cards,SUM(t.red_cards)red_cards,SUM(t.corners)corners,SUM(t.fouls)fouls FROM football_match_team_stat t JOIN t17_candidates c ON c.match_id=t.match_id WHERE t.is_deleted=0 GROUP BY c.league_id,c.season_id,t.team_id;
UPDATE football_team_competition_stat ts JOIN t17_team_totals x ON x.league_id=ts.league_id AND x.season_id=ts.season_id AND x.team_id=ts.team_id SET ts.shots=GREATEST(ts.shots,x.shots),ts.shots_on_target=GREATEST(ts.shots_on_target,x.shots_on_target),ts.yellow_cards=GREATEST(ts.yellow_cards,x.yellow_cards),ts.red_cards=GREATEST(ts.red_cards,x.red_cards),ts.corners=GREATEST(ts.corners,x.corners),ts.fouls=GREATEST(ts.fouls,x.fouls) WHERE ts.source='DEMO' AND ts.is_deleted=0;

DROP TEMPORARY TABLE IF EXISTS t17_rating_targets;
CREATE TEMPORARY TABLE t17_rating_targets AS SELECT match_id,player_id,official_rating FROM(SELECT p.match_id,p.player_id,p.official_rating,ROW_NUMBER()OVER(PARTITION BY p.match_id ORDER BY p.official_rating DESC,p.goals DESC,p.player_id)rn FROM football_match_player_stat p JOIN t17_candidates c ON c.match_id=p.match_id WHERE p.is_deleted=0)ranked WHERE rn=1;
SET @next_id=(SELECT GREATEST(32000000001000001,COALESCE(MAX(id)+1,32000000001000001)) FROM football_user_player_rating WHERE id>=32000000000000001 AND id<33000000000000001);
INSERT INTO football_user_player_rating(id,match_id,player_id,user_id,rating,status,is_deleted)
SELECT @next_id+ROW_NUMBER()OVER(ORDER BY u.id,p.match_id)-1,p.match_id,p.player_id,u.id,LEAST(10.0,GREATEST(1.0,ROUND((p.official_rating+(MOD(u.id,5)-2)*0.5)*2)/2)),'ACTIVE',0
FROM(SELECT id FROM sys_user WHERE id>=11000000000000001 AND username LIKE 'demo_user_%' AND role_type='USER' AND status='ACTIVE' AND is_deleted=0 ORDER BY id LIMIT 15)u CROSS JOIN t17_rating_targets p
WHERE NOT EXISTS(SELECT 1 FROM football_user_player_rating r WHERE r.user_id=u.id AND r.match_id=p.match_id AND r.player_id=p.player_id);

COMMIT;
SELECT 't17_candidate_matches' metric,COUNT(*) value FROM t17_candidates;
SELECT 't17_lineups' metric,COUNT(*) value FROM football_match_lineup WHERE is_deleted=0;
SELECT 't17_appearances' metric,COUNT(*) value FROM football_match_player_appearance WHERE is_deleted=0;
SELECT 't17_team_stats' metric,COUNT(*) value FROM football_match_team_stat WHERE is_deleted=0;
SELECT 't17_player_stats' metric,COUNT(*) value FROM football_match_player_stat WHERE is_deleted=0;
SELECT 't17_active_ratings' metric,COUNT(*) value FROM football_user_player_rating WHERE status='ACTIVE' AND is_deleted=0;
