USE south_stand;
SET NAMES utf8mb4;
SET collation_connection='utf8mb4_unicode_ci';

DROP PROCEDURE IF EXISTS t16_assert_safe;
DELIMITER $$
CREATE PROCEDURE t16_assert_safe()
BEGIN
  IF EXISTS (SELECT 1 FROM football_team_season_player WHERE id>=25000000000000001 AND id<26000000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_team_honor WHERE id>=26000000000000001 AND id<27000000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_player_team_history WHERE id>=27000000000000001 AND id<28000000000000001 AND source<>'DEMO') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='T16 conflict: reserved Demo ID range contains non-DEMO data';
  END IF;
  IF EXISTS (SELECT 1 FROM football_team_season_player r LEFT JOIN football_season s ON s.id=r.season_id LEFT JOIN team_player tp ON tp.team_id=r.team_id AND tp.player_id=r.player_id AND tp.status='ACTIVE' AND tp.is_deleted=0 WHERE r.source<>'DEMO' AND (s.id IS NULL OR s.league_id<>r.league_id OR tp.id IS NULL)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='T16 conflict: non-DEMO roster relation mismatch';
  END IF;
END$$
DELIMITER ;
CALL t16_assert_safe();
DROP PROCEDURE t16_assert_safe;

START TRANSACTION;

SET @t16_next_roster_id=(SELECT GREATEST(25000000001000001,COALESCE(MAX(id)+1,25000000001000001)) FROM football_team_season_player WHERE id>=25000000000000001 AND id<26000000000000001);
INSERT INTO football_team_season_player
  (id,league_id,season_id,team_id,player_id,position,shirt_number,squad_role,captain_flag,loan_flag,loan_from_team_id,joined_date,left_date,status,source,source_record_id,source_updated_at,is_deleted)
SELECT @t16_next_roster_id+ROW_NUMBER() OVER(ORDER BY x.season_id,x.team_id,x.player_id)-1,
       x.league_id,x.season_id,x.team_id,x.player_id,x.position,x.shirt_number,'FIRST_TEAM',
       IF(x.team_no=1 AND NOT EXISTS (SELECT 1 FROM football_team_season_player c WHERE c.season_id=x.season_id AND c.team_id=x.team_id AND c.captain_flag=1 AND c.status='ACTIVE' AND c.is_deleted=0),1,0),
       0,NULL,x.start_date,NULL,'ACTIVE','DEMO',CONCAT('t16-roster-',x.season_id,'-',x.team_id,'-',x.player_id),'2026-08-01 12:00:00',0
FROM (
  SELECT ps.league_id,ps.season_id,ps.team_id,ps.player_id,s.start_date,
         CASE COALESCE(tp.position,p.position) WHEN 'GK' THEN 'GOALKEEPER' WHEN 'DF' THEN 'DEFENDER' WHEN 'MF' THEN 'MIDFIELDER' ELSE 'FORWARD' END position,
         COALESCE(tp.shirt_number,p.shirt_number) shirt_number,
         ROW_NUMBER() OVER(PARTITION BY ps.season_id,ps.team_id ORDER BY ps.player_id) team_no
  FROM football_player_competition_stat ps
  JOIN football_season s ON s.id=ps.season_id AND s.league_id=ps.league_id AND s.status='ACTIVE' AND s.is_deleted=0
  JOIN team_player tp ON tp.team_id=ps.team_id AND tp.player_id=ps.player_id AND tp.status='ACTIVE' AND tp.is_deleted=0
  JOIN football_player p ON p.id=ps.player_id AND p.status='ACTIVE' AND p.is_deleted=0
  WHERE ps.is_deleted=0
) x
WHERE NOT EXISTS (SELECT 1 FROM football_team_season_player r WHERE r.season_id=x.season_id AND r.team_id=x.team_id AND r.player_id=x.player_id);

SET @t16_next_honor_id=(SELECT GREATEST(26000000001000001,COALESCE(MAX(id)+1,26000000001000001)) FROM football_team_honor WHERE id>=26000000000000001 AND id<27000000000000001);
INSERT INTO football_team_honor
  (id,team_id,league_id,honor_name,honor_type,title_count,winning_years,latest_year,source,source_record_id,is_deleted)
SELECT @t16_next_honor_id+ROW_NUMBER() OVER(ORDER BY t.id,h.slot)-1,t.id,NULL,h.honor_name,h.honor_type,1,
       CAST(2018+MOD(t.id+h.slot,7) AS CHAR),2018+MOD(t.id+h.slot,7),'DEMO',CONCAT('t16-honor-',t.id,'-',h.slot),0
FROM football_team t
JOIN (SELECT 1 slot,'Demo 城市杯' honor_name,'DOMESTIC_CUP' honor_type UNION ALL SELECT 2,'Demo 社区邀请赛','OTHER') h
WHERE t.status='ACTIVE' AND t.is_deleted=0
  AND NOT EXISTS (SELECT 1 FROM football_team_honor th WHERE th.team_id=t.id AND th.honor_name=h.honor_name AND th.honor_type=h.honor_type);

DROP TEMPORARY TABLE IF EXISTS t16_current_history;
CREATE TEMPORARY TABLE t16_current_history AS
SELECT * FROM (
  SELECT r.player_id,r.team_id,r.season_id,r.league_id,r.joined_date,r.shirt_number,r.position,r.loan_flag,
         ROW_NUMBER() OVER(PARTITION BY r.player_id ORDER BY s.current_flag DESC,s.start_date DESC,r.league_id ASC,r.season_id DESC) player_no
  FROM football_team_season_player r JOIN football_season s ON s.id=r.season_id
  WHERE r.status='ACTIVE' AND r.is_deleted=0
) ranked WHERE player_no=1;

SET @t16_next_history_id=(SELECT GREATEST(27000000001000001,COALESCE(MAX(id)+1,27000000001000001)) FROM football_player_team_history WHERE id>=27000000000000001 AND id<28000000000000001);
INSERT INTO football_player_team_history
  (id,player_id,team_id,season_id,start_date,end_date,shirt_number,position,appearances,goals,assists,current_flag,loan_flag,source,source_record_id,is_deleted)
SELECT @t16_next_history_id+ROW_NUMBER() OVER(ORDER BY r.player_id)-1,r.player_id,r.team_id,r.season_id,r.joined_date,NULL,r.shirt_number,r.position,
       COALESCE(ps.appearances,0),COALESCE(ps.goals,0),COALESCE(ps.assists,0),1,r.loan_flag,'DEMO',CONCAT('t16-current-',r.player_id),0
FROM t16_current_history r
LEFT JOIN football_player_competition_stat ps ON ps.player_id=r.player_id AND ps.team_id=r.team_id AND ps.season_id=r.season_id AND ps.league_id=r.league_id AND ps.is_deleted=0
WHERE NOT EXISTS (SELECT 1 FROM football_player_team_history h WHERE h.player_id=r.player_id AND h.team_id=r.team_id AND h.season_id=r.season_id);

COMMIT;

SELECT 't16_rosters' metric,COUNT(*) value FROM football_team_season_player
UNION ALL SELECT 't16_honors',COUNT(*) FROM football_team_honor
UNION ALL SELECT 't16_histories',COUNT(*) FROM football_player_team_history;
