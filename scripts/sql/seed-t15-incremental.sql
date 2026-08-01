USE south_stand;
SET NAMES utf8mb4;
SET collation_connection='utf8mb4_unicode_ci';

DROP PROCEDURE IF EXISTS t15_assert_safe;
DELIMITER $$
CREATE PROCEDURE t15_assert_safe()
BEGIN
  IF EXISTS (SELECT 1 FROM football_season WHERE status='ACTIVE' AND is_deleted=0 AND current_flag=1 GROUP BY league_id HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='T15 conflict: multiple active current seasons';
  END IF;
  IF EXISTS (SELECT 1 FROM football_season WHERE season_code='2025-2026' AND (start_date<>'2025-08-01' OR end_date<>'2026-06-30')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='T15 conflict: season_code date range mismatch';
  END IF;
  IF EXISTS (SELECT 1 FROM football_standing WHERE COALESCE(source,'')<>'DEMO' AND
      (played<>won+drawn+lost OR goal_difference<>goals_for-goals_against OR points<>won*3+drawn-deduction_points)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='T15 conflict: non-DEMO standing formula mismatch';
  END IF;
  IF EXISTS (SELECT 1 FROM football_player_competition_stat ps
      LEFT JOIN team_player tp ON tp.player_id=ps.player_id AND tp.team_id=ps.team_id AND tp.status='ACTIVE' AND tp.is_deleted=0
      WHERE COALESCE(ps.source,'')<>'DEMO' AND tp.id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='T15 conflict: non-DEMO player team relation mismatch';
  END IF;
  IF EXISTS (SELECT 1 FROM football_standing WHERE id>=22000000000000001 AND id<23000000000000001 AND COALESCE(source,'')<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_player_competition_stat WHERE id>=23000000000000001 AND id<24000000000000001 AND COALESCE(source,'')<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_team_competition_stat WHERE id>=24000000000000001 AND id<25000000000000001 AND COALESCE(source,'')<>'DEMO') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='T15 conflict: reserved Demo ID range contains non-DEMO data';
  END IF;
END$$
DELIMITER ;
CALL t15_assert_safe();
DROP PROCEDURE t15_assert_safe;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS t15_expected_season;
CREATE TEMPORARY TABLE t15_expected_season AS
SELECT l.id league_id, codes.season_code, codes.season_name, codes.start_date, codes.end_date,
       codes.current_flag
FROM football_league l
JOIN (
  SELECT 1 slot,'2024-2025' season_code,'2024/25赛季' season_name,DATE('2024-08-01') start_date,DATE('2025-06-30') end_date,0 current_flag
  UNION ALL
  SELECT 2,'2025-2026','2025/26赛季',DATE('2025-08-01'),DATE('2026-06-30'),1
) codes
WHERE l.status='ACTIVE' AND l.is_deleted=0;

SET @t15_next_season_id=(SELECT GREATEST(20000000001000001,COALESCE(MAX(id)+1,20000000001000001)) FROM football_season WHERE id>=20000000000000001 AND id<21000000000000001);

INSERT INTO football_season
  (id,league_id,season_code,season_name,start_date,end_date,current_flag,status,source,source_record_id,source_updated_at,synced_at,is_deleted)
SELECT @t15_next_season_id+ROW_NUMBER() OVER(ORDER BY e.league_id,e.season_code)-1,e.league_id,e.season_code,e.season_name,e.start_date,e.end_date,e.current_flag,'ACTIVE','DEMO',
       CONCAT('t15-incremental-',e.league_id,'-',e.season_code),'2026-07-20 12:00:00','2026-07-20 12:00:00',0
FROM t15_expected_season e
WHERE NOT EXISTS (SELECT 1 FROM football_season s WHERE s.league_id=e.league_id AND s.season_code=e.season_code);

SET @t15_next_stage_id=(SELECT GREATEST(21000000001000001,COALESCE(MAX(id)+1,21000000001000001)) FROM football_competition_stage WHERE id>=21000000000000001 AND id<22000000000000001);
INSERT INTO football_competition_stage
  (id,league_id,season_id,stage_type,stage_name,group_code,sort_order,status,is_deleted)
SELECT @t15_next_stage_id + ROW_NUMBER() OVER (ORDER BY s.league_id,s.id)-1,
       s.league_id,s.id,IF(l.league_type='CUP','GROUP','LEAGUE'),
       IF(l.league_type='CUP','小组赛','联赛阶段'),NULL,1,'ACTIVE',0
FROM football_season s JOIN football_league l ON l.id=s.league_id
WHERE s.status='ACTIVE' AND s.is_deleted=0
  AND NOT EXISTS (SELECT 1 FROM football_competition_stage st
                  WHERE st.league_id=s.league_id AND st.season_id=s.id AND st.status='ACTIVE' AND st.is_deleted=0);

DROP TEMPORARY TABLE IF EXISTS t15_team_league;
CREATE TEMPORARY TABLE t15_team_league (team_id BIGINT PRIMARY KEY, league_id BIGINT NOT NULL);
INSERT INTO t15_team_league(team_id,league_id)
SELECT team_id,league_id FROM (
  SELECT team_id,league_id,ROW_NUMBER() OVER(PARTITION BY team_id ORDER BY games DESC,league_id ASC) rn
  FROM (
    SELECT team_id,league_id,COUNT(*) games FROM (
      SELECT home_team_id team_id,league_id FROM match_info WHERE status='ACTIVE' AND is_deleted=0
      UNION ALL SELECT away_team_id,league_id FROM match_info WHERE status='ACTIVE' AND is_deleted=0
    ) appearances GROUP BY team_id,league_id
  ) counts
) ranked WHERE rn=1;

DROP TEMPORARY TABLE IF EXISTS t15_current_scope;
CREATE TEMPORARY TABLE t15_current_scope AS
SELECT s.league_id,s.id season_id,MIN(st.id) stage_id
FROM football_season s JOIN football_competition_stage st ON st.league_id=s.league_id AND st.season_id=s.id
WHERE s.current_flag=1 AND s.status='ACTIVE' AND s.is_deleted=0 AND st.status='ACTIVE' AND st.is_deleted=0
GROUP BY s.league_id,s.id;

DROP TEMPORARY TABLE IF EXISTS t15_missing_standing;
CREATE TEMPORARY TABLE t15_missing_standing AS
SELECT sc.league_id,sc.season_id,sc.stage_id,tl.team_id,
       ROW_NUMBER() OVER(ORDER BY sc.league_id,tl.team_id) global_no,
       ROW_NUMBER() OVER(PARTITION BY sc.league_id ORDER BY tl.team_id) team_no
FROM t15_current_scope sc JOIN t15_team_league tl ON tl.league_id=sc.league_id
WHERE NOT EXISTS (SELECT 1 FROM football_standing fs WHERE fs.league_id=sc.league_id AND fs.season_id=sc.season_id
                  AND fs.stage_id=sc.stage_id AND fs.group_code='');

SET @t15_next_standing_id=(SELECT GREATEST(22000000001000001,COALESCE(MAX(id)+1,22000000001000001)) FROM football_standing WHERE id>=22000000000000001 AND id<23000000000000001);

INSERT INTO football_standing
  (id,league_id,season_id,stage_id,group_code,team_id,rank_no,played,won,drawn,lost,goals_for,goals_against,
   goal_difference,points,deduction_points,form_text,source,source_updated_at,is_deleted)
SELECT @t15_next_standing_id+global_no-1,league_id,season_id,stage_id,'',team_id,team_no,20,
       GREATEST(4,14-team_no),MOD(team_no,3),20-GREATEST(4,14-team_no)-MOD(team_no,3),
       44-team_no*3,15+team_no*2,(44-team_no*3)-(15+team_no*2),
       GREATEST(4,14-team_no)*3+MOD(team_no,3),0,'胜胜平负胜','DEMO','2026-07-20 12:00:00',0
FROM t15_missing_standing;

SET @t15_next_team_stat_id=(SELECT GREATEST(24000000001000001,COALESCE(MAX(id)+1,24000000001000001)) FROM football_team_competition_stat WHERE id>=24000000000000001 AND id<25000000000000001);
INSERT INTO football_team_competition_stat
  (id,league_id,season_id,stage_id,team_id,played,goals_for,goals_against,assists,yellow_cards,red_cards,
   shots,shots_on_target,corners,fouls,clean_sheets,avg_rating,source,source_updated_at,is_deleted)
SELECT @t15_next_team_stat_id+ROW_NUMBER() OVER(ORDER BY fs.league_id,fs.team_id)-1,
       fs.league_id,fs.season_id,fs.stage_id,fs.team_id,fs.played,fs.goals_for,fs.goals_against,
       GREATEST(0,fs.goals_for-3),22+fs.rank_no,MOD(fs.rank_no,3),fs.goals_for*5+fs.rank_no*3,
       fs.goals_for*2+fs.rank_no,70+fs.rank_no*3,180+fs.rank_no*4,GREATEST(0,8-fs.rank_no),
       7.45-fs.rank_no*0.06,'DEMO','2026-07-20 12:00:00',0
FROM football_standing fs JOIN t15_current_scope sc ON sc.league_id=fs.league_id AND sc.season_id=fs.season_id AND sc.stage_id=fs.stage_id
WHERE fs.source='DEMO' AND fs.is_deleted=0 AND NOT EXISTS (
  SELECT 1 FROM football_team_competition_stat ts WHERE ts.league_id=fs.league_id AND ts.season_id=fs.season_id
  AND ts.stage_id=fs.stage_id AND ts.team_id=fs.team_id);

SET @t15_next_player_stat_id=(SELECT GREATEST(23000000001000001,COALESCE(MAX(id)+1,23000000001000001)) FROM football_player_competition_stat WHERE id>=23000000000000001 AND id<24000000000000001);
INSERT INTO football_player_competition_stat
  (id,league_id,season_id,stage_id,player_id,team_id,appearances,starts,minutes,goals,assists,yellow_cards,
   red_cards,shots,shots_on_target,saves,rating,source,source_updated_at,is_deleted)
SELECT @t15_next_player_stat_id+ROW_NUMBER() OVER(ORDER BY sc.league_id,tp.team_id,tp.player_id)-1,
       sc.league_id,sc.season_id,sc.stage_id,tp.player_id,tp.team_id,
       14+MOD(tp.player_id,7),12+MOD(tp.player_id,3),(12+MOD(tp.player_id,3))*82,
       IF(p.position IN ('FW','MF'),2+MOD(tp.player_id,9),MOD(tp.player_id,2)),
       IF(p.position IN ('FW','MF'),1+MOD(tp.player_id,7),0),MOD(tp.player_id,6),IF(MOD(tp.player_id,19)=0,1,0),
       18+MOD(tp.player_id,24),12+MOD(tp.player_id,10),IF(p.position='GK',30+MOD(tp.player_id,20),0),
       6.30+MOD(tp.player_id,150)/100,'DEMO','2026-07-20 12:00:00',0
FROM team_player tp JOIN football_player p ON p.id=tp.player_id AND p.status='ACTIVE' AND p.is_deleted=0
JOIN t15_team_league tl ON tl.team_id=tp.team_id
JOIN t15_current_scope sc ON sc.league_id=tl.league_id
WHERE tp.status='ACTIVE' AND tp.is_deleted=0 AND NOT EXISTS (
  SELECT 1 FROM football_player_competition_stat ps WHERE ps.league_id=sc.league_id AND ps.season_id=sc.season_id
  AND ps.stage_id=sc.stage_id AND ps.player_id=tp.player_id AND ps.team_id=tp.team_id);

-- Repair only rows previously generated by this incremental T15 seed and only when its own invariant is broken.
UPDATE football_player_competition_stat
SET shots_on_target=GREATEST(shots_on_target,goals)
WHERE id>=23000000001000001 AND id<24000000000000001 AND source='DEMO'
  AND source_updated_at='2026-07-20 12:00:00' AND goals>shots_on_target;

COMMIT;

SELECT 't15_incremental_seasons' metric,COUNT(*) value FROM football_season
UNION ALL SELECT 't15_incremental_standings',COUNT(*) FROM football_standing
UNION ALL SELECT 't15_incremental_player_stats',COUNT(*) FROM football_player_competition_stat
UNION ALL SELECT 't15_incremental_team_stats',COUNT(*) FROM football_team_competition_stat;
