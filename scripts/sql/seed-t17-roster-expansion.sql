USE south_stand;
SET NAMES utf8mb4;
SET collation_connection='utf8mb4_unicode_ci';

DROP PROCEDURE IF EXISTS t17_roster_assert_safe;
DELIMITER $$
CREATE PROCEDURE t17_roster_assert_safe()
BEGIN
  IF EXISTS (SELECT 1 FROM football_player WHERE id>=33000000000000001 AND id<33100000000000001 AND remark NOT LIKE 'T17_DEMO_ROSTER:%') OR
     EXISTS (SELECT 1 FROM team_player WHERE id>=33100000000000001 AND id<33200000000000001 AND remark NOT LIKE 'T17_DEMO_ROSTER:%') OR
     EXISTS (SELECT 1 FROM football_team_season_player WHERE id>=33200000000000001 AND id<33300000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_player_team_history WHERE id>=33300000000000001 AND id<33400000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_player_competition_stat WHERE id>=33400000000000001 AND id<33500000000000001 AND source<>'DEMO') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='T17 roster conflict: reserved ID range contains non-DEMO data';
  END IF;
END$$
DELIMITER ;
CALL t17_roster_assert_safe();
DROP PROCEDURE t17_roster_assert_safe;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS t17_safe_matches;
CREATE TEMPORARY TABLE t17_safe_matches AS
SELECT m.id match_id,m.league_id,s.id season_id,m.home_team_id,m.away_team_id,m.match_time
FROM match_info m
JOIN football_season s ON s.league_id=m.league_id AND s.current_flag=1 AND s.status='ACTIVE' AND s.is_deleted=0
JOIN football_team h ON h.id=m.home_team_id AND h.status='ACTIVE' AND h.is_deleted=0
JOIN football_team a ON a.id=m.away_team_id AND a.status='ACTIVE' AND a.is_deleted=0
WHERE m.match_status='FINISHED' AND m.status='ACTIVE' AND m.is_deleted=0 AND m.home_score>=0 AND m.away_score>=0
  AND m.home_score=(SELECT COUNT(*) FROM match_event e WHERE e.match_id=m.id AND e.team_id=m.home_team_id AND e.event_type='GOAL' AND e.status='ACTIVE' AND e.is_deleted=0)
  AND m.away_score=(SELECT COUNT(*) FROM match_event e WHERE e.match_id=m.id AND e.team_id=m.away_team_id AND e.event_type='GOAL' AND e.status='ACTIVE' AND e.is_deleted=0)
  AND NOT EXISTS (
    SELECT 1 FROM match_event e
    LEFT JOIN team_player tp ON tp.team_id=e.team_id AND tp.player_id=e.player_id AND tp.status='ACTIVE' AND tp.is_deleted=0
    LEFT JOIN football_team_season_player r ON r.season_id=s.id AND r.team_id=e.team_id AND r.player_id=e.player_id AND r.status='ACTIVE' AND r.is_deleted=0
    WHERE e.match_id=m.id AND e.status='ACTIVE' AND e.is_deleted=0
      AND (e.team_id IS NULL OR e.team_id NOT IN(m.home_team_id,m.away_team_id) OR e.player_id IS NULL OR tp.id IS NULL OR r.id IS NULL)
  )
  AND NOT EXISTS (
    SELECT 1 FROM match_event e
    LEFT JOIN team_player tp ON tp.team_id=e.team_id AND tp.player_id=e.assist_player_id AND tp.status='ACTIVE' AND tp.is_deleted=0
    WHERE e.match_id=m.id AND e.status='ACTIVE' AND e.is_deleted=0 AND e.assist_player_id IS NOT NULL AND tp.id IS NULL
  );

DROP TEMPORARY TABLE IF EXISTS t17_target_leagues;
CREATE TEMPORARY TABLE t17_target_leagues AS
SELECT league_id FROM t17_safe_matches GROUP BY league_id
HAVING COUNT(*)>=8
ORDER BY COUNT(*) DESC,COUNT(DISTINCT home_team_id)+COUNT(DISTINCT away_team_id),league_id LIMIT 3;

DROP TEMPORARY TABLE IF EXISTS t17_target_matches;
CREATE TEMPORARY TABLE t17_target_matches AS
SELECT s.* FROM t17_safe_matches s JOIN t17_target_leagues l ON l.league_id=s.league_id
ORDER BY s.league_id,s.match_time,s.match_id LIMIT 20;

DROP TEMPORARY TABLE IF EXISTS t17_target_teams;
CREATE TEMPORARY TABLE t17_target_teams AS
SELECT DISTINCT m.league_id,m.season_id,IF(side.n=0,m.home_team_id,m.away_team_id) team_id
FROM t17_target_matches m CROSS JOIN (SELECT 0 n UNION ALL SELECT 1) side;

DROP TEMPORARY TABLE IF EXISTS t17_new_slots;
CREATE TEMPORARY TABLE t17_new_slots AS
SELECT 33000000000000001+ROW_NUMBER() OVER(ORDER BY t.season_id,t.team_id,p.pos_order,n.n)-1 player_id,
       t.league_id,t.season_id,t.team_id,p.position,p.label,n.n position_no,
       CASE p.position WHEN 'GOALKEEPER' THEN 20+n.n WHEN 'DEFENDER' THEN 30+n.n WHEN 'MIDFIELDER' THEN 40+n.n ELSE 50+n.n END shirt_number
FROM t17_target_teams t
CROSS JOIN (
  SELECT 'GOALKEEPER' position,'门将' label,1 pos_order,2 target_count
  UNION ALL SELECT 'DEFENDER','后卫',2,6
  UNION ALL SELECT 'MIDFIELDER','中场',3,6
  UNION ALL SELECT 'FORWARD','前锋',4,4
) p
CROSS JOIN (SELECT 1 n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) n
WHERE n.n<=p.target_count
  AND n.n>(SELECT COUNT(*) FROM football_team_season_player r WHERE r.season_id=t.season_id AND r.team_id=t.team_id AND r.position=p.position AND r.status='ACTIVE' AND r.is_deleted=0);

INSERT INTO football_player(id,player_name,player_name_en,avatar_url,nationality,shirt_number,position,birth_date,height_cm,weight_kg,market_value,retired,follower_count,status,is_deleted,remark)
SELECT n.player_id,CONCAT(t.team_name,'演示',n.label,LPAD(n.position_no,2,'0')),
       CONCAT('T17 Demo ',REPLACE(n.position,'GOALKEEPER','Goalkeeper'),' ',n.position_no),
       COALESCE((SELECT p.avatar_url FROM football_player p WHERE p.avatar_url IS NOT NULL AND p.avatar_url<>'' AND p.status='ACTIVE' AND p.is_deleted=0 ORDER BY p.id LIMIT 1),'/api/public/files/demo-player.png'),
       COALESCE(t.country,'中国'),n.shirt_number,
       CASE n.position WHEN 'GOALKEEPER' THEN 'GK' WHEN 'DEFENDER' THEN 'DF' WHEN 'MIDFIELDER' THEN 'MF' ELSE 'FW' END,
       DATE_ADD('1996-01-01',INTERVAL MOD(n.player_id,2500) DAY),
       CASE n.position WHEN 'GOALKEEPER' THEN 190 WHEN 'DEFENDER' THEN 184 WHEN 'MIDFIELDER' THEN 178 ELSE 181 END,
       CASE n.position WHEN 'GOALKEEPER' THEN 82 WHEN 'DEFENDER' THEN 78 WHEN 'MIDFIELDER' THEN 72 ELSE 75 END,
       'Demo',0,0,'ACTIVE',0,CONCAT('T17_DEMO_ROSTER:',n.season_id,':',n.team_id,':',n.position,':',n.position_no)
FROM t17_new_slots n JOIN football_team t ON t.id=n.team_id
WHERE NOT EXISTS (SELECT 1 FROM football_player p WHERE p.id=n.player_id OR p.remark=CONCAT('T17_DEMO_ROSTER:',n.season_id,':',n.team_id,':',n.position,':',n.position_no));

INSERT INTO team_player(id,team_id,player_id,team_type,season,shirt_number,position,status,is_deleted,remark)
SELECT 33100000000000001+(n.player_id-33000000000000001),n.team_id,n.player_id,'CLUB',s.season_code,n.shirt_number,
       CASE n.position WHEN 'GOALKEEPER' THEN 'GK' WHEN 'DEFENDER' THEN 'DF' WHEN 'MIDFIELDER' THEN 'MF' ELSE 'FW' END,
       'ACTIVE',0,CONCAT('T17_DEMO_ROSTER:',n.season_id,':',n.team_id,':',n.player_id)
FROM t17_new_slots n JOIN football_season s ON s.id=n.season_id
WHERE NOT EXISTS (SELECT 1 FROM team_player tp WHERE tp.team_id=n.team_id AND tp.player_id=n.player_id AND tp.status='ACTIVE' AND tp.is_deleted=0);

INSERT INTO football_team_season_player(id,league_id,season_id,team_id,player_id,position,shirt_number,squad_role,captain_flag,loan_flag,joined_date,status,source,source_record_id,is_deleted)
SELECT 33200000000000001+(n.player_id-33000000000000001),n.league_id,n.season_id,n.team_id,n.player_id,n.position,n.shirt_number,'FIRST_TEAM',0,0,s.start_date,'ACTIVE','DEMO',CONCAT('t17-roster-',n.season_id,'-',n.team_id,'-',n.player_id),0
FROM t17_new_slots n JOIN football_season s ON s.id=n.season_id
WHERE NOT EXISTS (SELECT 1 FROM football_team_season_player r WHERE r.season_id=n.season_id AND r.team_id=n.team_id AND r.player_id=n.player_id);

INSERT INTO football_player_team_history(id,player_id,team_id,season_id,start_date,end_date,shirt_number,position,appearances,goals,assists,current_flag,loan_flag,source,source_record_id,is_deleted)
SELECT 33300000000000001+(n.player_id-33000000000000001),n.player_id,n.team_id,n.season_id,s.start_date,NULL,n.shirt_number,n.position,20,0,0,1,0,'DEMO',CONCAT('t17-history-',n.season_id,'-',n.team_id,'-',n.player_id),0
FROM t17_new_slots n JOIN football_season s ON s.id=n.season_id
WHERE NOT EXISTS (SELECT 1 FROM football_player_team_history h WHERE h.player_id=n.player_id AND h.team_id=n.team_id AND h.season_id=n.season_id);

INSERT INTO football_player_competition_stat(id,league_id,season_id,stage_id,player_id,team_id,appearances,starts,minutes,goals,assists,yellow_cards,red_cards,shots,shots_on_target,saves,rating,source,is_deleted)
SELECT 33400000000000001+(n.player_id-33000000000000001),n.league_id,n.season_id,0,n.player_id,n.team_id,20,20,1800,0,0,0,0,40,20,
       IF(n.position='GOALKEEPER',40,0),6.50,'DEMO',0
FROM t17_new_slots n
WHERE NOT EXISTS (SELECT 1 FROM football_player_competition_stat ps WHERE ps.league_id=n.league_id AND ps.season_id=n.season_id AND ps.stage_id=0 AND ps.player_id=n.player_id AND ps.team_id=n.team_id);

COMMIT;

SELECT 't17_target_matches' metric,COUNT(*) value FROM t17_target_matches;
SELECT 't17_target_teams' metric,COUNT(*) value FROM t17_target_teams;
SELECT 't17_new_players' metric,COUNT(*) value FROM football_player WHERE id>=33000000000000001 AND id<33100000000000001 AND is_deleted=0;
SELECT 't17_roster_teams_at_18' metric,COUNT(*) value FROM (
  SELECT t.team_id FROM t17_target_teams t JOIN football_team_season_player r ON r.season_id=t.season_id AND r.team_id=t.team_id AND r.status='ACTIVE' AND r.is_deleted=0
  GROUP BY t.season_id,t.team_id HAVING COUNT(*)>=18 AND SUM(r.position='GOALKEEPER')>=2 AND SUM(r.position='DEFENDER')>=5 AND SUM(r.position='MIDFIELDER')>=5 AND SUM(r.position='FORWARD')>=3
) ok;
