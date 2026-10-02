USE south_stand;
SET NAMES utf8mb4;
SET collation_connection='utf8mb4_unicode_ci';

-- P1-M2 fixed DEMO target:
-- match 15000000000000060: Juventus 2-0 AC Milan
-- league 12000000000000004, season 20000000000000008, stage 21000000000000008
-- This seed is additive, idempotent and limited to the reserved P1-M2 ranges below.

DROP PROCEDURE IF EXISTS p1_m2_assert_safe;
DELIMITER $$
CREATE PROCEDURE p1_m2_assert_safe()
BEGIN
  IF EXISTS (SELECT 1 FROM football_player WHERE id>=36000000000000001 AND id<36100000000000001 AND (remark IS NULL OR remark NOT LIKE 'P1_M2_DEMO_ROSTER:%')) OR
     EXISTS (SELECT 1 FROM team_player WHERE id>=36100000000000001 AND id<36200000000000001 AND (remark IS NULL OR remark NOT LIKE 'P1_M2_DEMO_ROSTER:%')) OR
     EXISTS (SELECT 1 FROM football_team_season_player WHERE id>=36200000000000001 AND id<36300000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_player_team_history WHERE id>=36300000000000001 AND id<36400000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_player_competition_stat WHERE id>=36400000000000001 AND id<36500000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_match_lineup WHERE id>=36500000000000001 AND id<36600000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_match_player_appearance WHERE id>=36600000000000001 AND id<36700000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_match_team_stat WHERE id>=36700000000000001 AND id<36800000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_match_player_stat WHERE id>=36800000000000001 AND id<36900000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM match_event WHERE id>=36900000000000001 AND id<37000000000000001 AND (remark IS NULL OR remark NOT LIKE 'P1-M2-event-%')) OR
     EXISTS (SELECT 1 FROM football_user_player_rating WHERE id>=37000000000000001 AND id<37100000000000001 AND NOT (match_id=15000000000000060 AND user_id IN(11000000000000001,11000000000000002,11000000000000003))) OR
     EXISTS (SELECT 1 FROM football_standing WHERE id>=37100000000000001 AND id<37200000000000001 AND source<>'DEMO') OR
     EXISTS (SELECT 1 FROM football_team_competition_stat WHERE id>=37200000000000001 AND id<37300000000000001 AND source<>'DEMO') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='P1-M2 conflict: reserved range contains non-P1-M2 data';
  END IF;
END$$
DELIMITER ;
CALL p1_m2_assert_safe();
DROP PROCEDURE p1_m2_assert_safe;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS p1_m2_roster;
CREATE TEMPORARY TABLE p1_m2_roster (
  seq INT PRIMARY KEY,
  team_id BIGINT NOT NULL,
  position_short VARCHAR(8) NOT NULL,
  position_full VARCHAR(16) NOT NULL,
  shirt_number INT NOT NULL,
  local_number INT NOT NULL
);
INSERT INTO p1_m2_roster(seq,team_id,position_short,position_full,shirt_number,local_number) VALUES
  (1,13000000000000011,'GK','GOALKEEPER',20,1),(2,13000000000000011,'DF','DEFENDER',21,2),(3,13000000000000011,'DF','DEFENDER',22,3),(4,13000000000000011,'DF','DEFENDER',23,4),(5,13000000000000011,'DF','DEFENDER',24,5),
  (6,13000000000000011,'MF','MIDFIELDER',25,6),(7,13000000000000011,'MF','MIDFIELDER',26,7),(8,13000000000000011,'MF','MIDFIELDER',27,8),(9,13000000000000011,'MF','MIDFIELDER',28,9),
  (10,13000000000000011,'FW','FORWARD',29,10),(11,13000000000000011,'FW','FORWARD',30,11),(12,13000000000000011,'FW','FORWARD',31,12),
  (13,13000000000000012,'GK','GOALKEEPER',20,1),(14,13000000000000012,'DF','DEFENDER',21,2),(15,13000000000000012,'DF','DEFENDER',22,3),(16,13000000000000012,'DF','DEFENDER',23,4),(17,13000000000000012,'DF','DEFENDER',24,5),
  (18,13000000000000012,'MF','MIDFIELDER',25,6),(19,13000000000000012,'MF','MIDFIELDER',26,7),(20,13000000000000012,'MF','MIDFIELDER',27,8),(21,13000000000000012,'MF','MIDFIELDER',28,9),
  (22,13000000000000012,'FW','FORWARD',29,10),(23,13000000000000012,'FW','FORWARD',30,11),(24,13000000000000012,'FW','FORWARD',31,12);

INSERT INTO football_player(id,player_name,player_name_en,avatar_url,nationality,shirt_number,position,birth_date,height_cm,weight_kg,market_value,retired,follower_count,status,is_deleted,remark)
SELECT 36000000000000000+r.seq,CONCAT(t.team_name,' M2 ',CASE r.position_short WHEN 'GK' THEN '门将' WHEN 'DF' THEN '后卫' WHEN 'MF' THEN '中场' ELSE '前锋' END,LPAD(r.local_number,2,'0')),
       CONCAT('P1 M2 ',t.team_name,' ',r.position_short,' ',r.local_number),'/demo/p1-media/player-flagship.png',IF(t.id=13000000000000011,'意大利','意大利'),r.shirt_number,r.position_short,
       DATE_ADD('1995-01-01',INTERVAL MOD(r.seq*97,3000) DAY),CASE r.position_short WHEN 'GK' THEN 190 WHEN 'DF' THEN 184 WHEN 'MF' THEN 178 ELSE 181 END,
       CASE r.position_short WHEN 'GK' THEN 82 WHEN 'DF' THEN 78 WHEN 'MF' THEN 72 ELSE 75 END,'Demo',0,0,'ACTIVE',0,
       CONCAT('P1_M2_DEMO_ROSTER:',r.team_id,':',r.seq)
FROM p1_m2_roster r JOIN football_team t ON t.id=r.team_id
WHERE NOT EXISTS (SELECT 1 FROM football_player p WHERE p.id=36000000000000000+r.seq);

INSERT INTO team_player(id,team_id,player_id,team_type,season,shirt_number,position,status,is_deleted,remark)
SELECT 36100000000000000+r.seq,r.team_id,36000000000000000+r.seq,'CLUB','2025-2026',r.shirt_number,r.position_short,'ACTIVE',0,
       CONCAT('P1_M2_DEMO_ROSTER:',r.team_id,':',r.seq)
FROM p1_m2_roster r
WHERE NOT EXISTS (SELECT 1 FROM team_player tp WHERE tp.id=36100000000000000+r.seq);

INSERT INTO football_team_season_player(id,league_id,season_id,team_id,player_id,position,shirt_number,squad_role,captain_flag,loan_flag,joined_date,status,source,source_record_id,is_deleted)
SELECT 36200000000000000+r.seq,12000000000000004,20000000000000008,r.team_id,36000000000000000+r.seq,r.position_full,r.shirt_number,'FIRST_TEAM',0,0,'2025-08-01','ACTIVE','DEMO',
       CONCAT('P1-M2-roster-',r.team_id,'-',r.seq),0
FROM p1_m2_roster r
WHERE NOT EXISTS (SELECT 1 FROM football_team_season_player x WHERE x.id=36200000000000000+r.seq);

INSERT INTO football_player_team_history(id,player_id,team_id,season_id,start_date,end_date,shirt_number,position,appearances,goals,assists,current_flag,loan_flag,source,source_record_id,is_deleted)
SELECT 36300000000000000+r.seq,36000000000000000+r.seq,r.team_id,20000000000000008,'2025-08-01',NULL,r.shirt_number,r.position_full,20,
       CASE r.position_short WHEN 'FW' THEN 6+MOD(r.seq,4) WHEN 'MF' THEN 2+MOD(r.seq,3) ELSE 0 END,
       CASE r.position_short WHEN 'FW' THEN 3 ELSE MOD(r.seq,3) END,1,0,'DEMO',CONCAT('P1-M2-history-',r.team_id,'-',r.seq),0
FROM p1_m2_roster r
WHERE NOT EXISTS (SELECT 1 FROM football_player_team_history x WHERE x.id=36300000000000000+r.seq);

INSERT INTO football_player_competition_stat(id,league_id,season_id,stage_id,player_id,team_id,appearances,starts,minutes,goals,assists,yellow_cards,red_cards,shots,shots_on_target,saves,rating,source,is_deleted)
SELECT 36400000000000000+r.seq,12000000000000004,20000000000000008,21000000000000008,36000000000000000+r.seq,r.team_id,20,15,CASE r.position_short WHEN 'GK' THEN 1260 WHEN 'DF' THEN 1350 WHEN 'MF' THEN 1440 ELSE 1320 END,
       CASE r.position_short WHEN 'FW' THEN 6+MOD(r.seq,4) WHEN 'MF' THEN 2+MOD(r.seq,3) ELSE 0 END,
       CASE r.position_short WHEN 'FW' THEN 3 ELSE MOD(r.seq,3) END,MOD(r.seq,3),0,CASE r.position_short WHEN 'FW' THEN 42 ELSE 20+MOD(r.seq,12) END,
       CASE r.position_short WHEN 'FW' THEN 24 ELSE 10+MOD(r.seq,8) END,IF(r.position_short='GK',40+MOD(r.seq,5),0),ROUND(6.80+MOD(r.seq,9)*0.08,2),'DEMO',0
FROM p1_m2_roster r
WHERE NOT EXISTS (SELECT 1 FROM football_player_competition_stat x WHERE x.id=36400000000000000+r.seq);

INSERT INTO football_match_lineup(id,match_id,team_id,formation,coach_name,confirmed_flag,source,source_record_id,is_deleted)
SELECT 36500000000000000,15000000000000060,13000000000000012,'4-3-3',coach_name,1,'DEMO','P1-M2-lineup-15000000000000060-13000000000000012',0 FROM football_team WHERE id=13000000000000012
  AND NOT EXISTS (SELECT 1 FROM football_match_lineup x WHERE x.id=36500000000000000)
UNION ALL
SELECT 36500000000000001,15000000000000060,13000000000000011,'4-3-3',coach_name,1,'DEMO','P1-M2-lineup-15000000000000060-13000000000000011',0 FROM football_team WHERE id=13000000000000011
  AND NOT EXISTS (SELECT 1 FROM football_match_lineup x WHERE x.id=36500000000000001);

DROP TEMPORARY TABLE IF EXISTS p1_m2_appearance_seed;
CREATE TEMPORARY TABLE p1_m2_appearance_seed (
  seq INT PRIMARY KEY,team_id BIGINT,player_id BIGINT,lineup_type VARCHAR(16),position VARCHAR(16),shirt_number INT,captain_flag TINYINT,started_flag TINYINT,appeared_flag TINYINT,start_minute INT,end_minute INT,sub_in INT,sub_out INT,field_x DECIMAL(5,2),field_y DECIMAL(5,2)
);
INSERT INTO p1_m2_appearance_seed VALUES
  (1,13000000000000012,14000000000000067,'STARTER','GOALKEEPER',1,1,1,1,0,90,NULL,NULL,8,50),(2,13000000000000012,14000000000000068,'STARTER','DEFENDER',2,0,1,1,0,74,NULL,74,25,15),(3,13000000000000012,14000000000000069,'STARTER','DEFENDER',5,0,1,1,0,90,NULL,NULL,25,38),(4,13000000000000012,36000000000000014,'STARTER','DEFENDER',21,0,1,1,0,90,NULL,NULL,25,62),(5,13000000000000012,36000000000000015,'STARTER','DEFENDER',22,0,1,1,0,90,NULL,NULL,25,85),(6,13000000000000012,14000000000000070,'STARTER','MIDFIELDER',8,0,1,1,0,67,NULL,67,50,25),(7,13000000000000012,14000000000000071,'STARTER','MIDFIELDER',10,0,1,1,0,90,NULL,NULL,50,50),(8,13000000000000012,36000000000000018,'STARTER','MIDFIELDER',25,0,1,1,0,90,NULL,NULL,50,75),(9,13000000000000012,14000000000000072,'STARTER','FORWARD',9,0,1,1,0,82,NULL,82,78,25),(10,13000000000000012,36000000000000022,'STARTER','FORWARD',29,0,1,1,0,90,NULL,NULL,78,50),(11,13000000000000012,36000000000000023,'STARTER','FORWARD',30,0,1,1,0,90,NULL,NULL,78,75),(12,13000000000000012,36000000000000013,'SUBSTITUTE','GOALKEEPER',20,0,0,0,NULL,NULL,NULL,NULL,8,50),(13,13000000000000012,36000000000000016,'SUBSTITUTE','DEFENDER',23,0,0,1,74,90,74,NULL,25,15),(14,13000000000000012,36000000000000017,'SUBSTITUTE','DEFENDER',24,0,0,0,NULL,NULL,NULL,NULL,25,38),(15,13000000000000012,36000000000000019,'SUBSTITUTE','MIDFIELDER',26,0,0,1,67,90,67,NULL,50,25),(16,13000000000000012,36000000000000020,'SUBSTITUTE','MIDFIELDER',27,0,0,0,NULL,NULL,NULL,NULL,50,50),(17,13000000000000012,36000000000000021,'SUBSTITUTE','MIDFIELDER',28,0,0,0,NULL,NULL,NULL,NULL,50,75),(18,13000000000000012,36000000000000024,'SUBSTITUTE','FORWARD',31,0,0,1,82,90,82,NULL,78,50),
  (19,13000000000000011,14000000000000061,'STARTER','GOALKEEPER',1,1,1,1,0,90,NULL,NULL,8,50),(20,13000000000000011,14000000000000062,'STARTER','DEFENDER',2,0,1,1,0,80,NULL,80,25,15),(21,13000000000000011,14000000000000063,'STARTER','DEFENDER',5,0,1,1,0,90,NULL,NULL,25,38),(22,13000000000000011,36000000000000002,'STARTER','DEFENDER',21,0,1,1,0,90,NULL,NULL,25,62),(23,13000000000000011,36000000000000003,'STARTER','DEFENDER',22,0,1,1,0,90,NULL,NULL,25,85),(24,13000000000000011,14000000000000064,'STARTER','MIDFIELDER',8,0,1,1,0,63,NULL,63,50,25),(25,13000000000000011,14000000000000065,'STARTER','MIDFIELDER',10,0,1,1,0,90,NULL,NULL,50,50),(26,13000000000000011,36000000000000006,'STARTER','MIDFIELDER',25,0,1,1,0,90,NULL,NULL,50,75),(27,13000000000000011,14000000000000066,'STARTER','FORWARD',9,0,1,1,0,70,NULL,70,78,25),(28,13000000000000011,36000000000000010,'STARTER','FORWARD',29,0,1,1,0,90,NULL,NULL,78,50),(29,13000000000000011,36000000000000011,'STARTER','FORWARD',30,0,1,1,0,90,NULL,NULL,78,75),(30,13000000000000011,36000000000000001,'SUBSTITUTE','GOALKEEPER',20,0,0,0,NULL,NULL,NULL,NULL,8,50),(31,13000000000000011,36000000000000004,'SUBSTITUTE','DEFENDER',23,0,0,1,80,90,80,NULL,25,15),(32,13000000000000011,36000000000000005,'SUBSTITUTE','DEFENDER',24,0,0,0,NULL,NULL,NULL,NULL,25,38),(33,13000000000000011,36000000000000007,'SUBSTITUTE','MIDFIELDER',26,0,0,1,63,90,63,NULL,50,25),(34,13000000000000011,36000000000000008,'SUBSTITUTE','MIDFIELDER',27,0,0,0,NULL,NULL,NULL,NULL,50,50),(35,13000000000000011,36000000000000009,'SUBSTITUTE','MIDFIELDER',28,0,0,0,NULL,NULL,NULL,NULL,50,75),(36,13000000000000011,36000000000000012,'SUBSTITUTE','FORWARD',31,0,0,1,70,90,70,NULL,78,50);

INSERT INTO football_match_player_appearance(id,match_id,team_id,player_id,lineup_type,position,shirt_number,captain_flag,started_flag,appeared_flag,start_minute,end_minute,substituted_in_minute,substituted_out_minute,field_x,field_y,status,source,source_record_id,is_deleted)
SELECT 36600000000000000+s.seq,15000000000000060,s.team_id,s.player_id,s.lineup_type,s.position,s.shirt_number,s.captain_flag,s.started_flag,s.appeared_flag,s.start_minute,s.end_minute,s.sub_in,s.sub_out,s.field_x,s.field_y,'ACTIVE','DEMO',CONCAT('P1-M2-appearance-',s.seq),0
FROM p1_m2_appearance_seed s
WHERE NOT EXISTS (SELECT 1 FROM football_match_player_appearance x WHERE x.id=36600000000000000+s.seq);

DROP TEMPORARY TABLE IF EXISTS p1_m2_event_seed;
CREATE TEMPORARY TABLE p1_m2_event_seed (id BIGINT PRIMARY KEY,team_id BIGINT,player_id BIGINT,assist_player_id BIGINT,event_type VARCHAR(32),minute INT,description VARCHAR(512));
INSERT INTO p1_m2_event_seed VALUES
  (36900000000000001,13000000000000012,14000000000000068,NULL,'SUBSTITUTION',74,'尤文图斯换人：16号球员替换下场球员'),
  (36900000000000002,13000000000000012,14000000000000072,NULL,'SUBSTITUTION',82,'尤文图斯换人：24号球员替换下场球员'),
  (36900000000000003,13000000000000011,14000000000000064,NULL,'SUBSTITUTION',63,'AC米兰换人：7号球员替换下场球员'),
  (36900000000000004,13000000000000011,14000000000000066,NULL,'SUBSTITUTION',70,'AC米兰换人：12号球员替换下场球员'),
  (36900000000000005,13000000000000011,14000000000000062,NULL,'SUBSTITUTION',80,'AC米兰换人：4号球员替换下场球员'),
  (36900000000000006,13000000000000012,14000000000000069,NULL,'YELLOW_CARD',54,'尤文图斯防守犯规，黄牌'),
  (36900000000000007,13000000000000011,14000000000000061,NULL,'YELLOW_CARD',77,'AC米兰拖延比赛，黄牌');
INSERT INTO match_event(id,match_id,team_id,player_id,assist_player_id,event_type,minute,score_after,description,has_debate,status,is_deleted,remark)
SELECT e.id,15000000000000060,e.team_id,e.player_id,e.assist_player_id,e.event_type,e.minute,NULL,e.description,0,'ACTIVE',0,CONCAT('P1-M2-event-',e.id)
FROM p1_m2_event_seed e
WHERE NOT EXISTS (SELECT 1 FROM match_event x WHERE x.id=e.id);

INSERT INTO football_match_player_stat(id,match_id,team_id,player_id,minutes,goals,assists,shots,shots_on_target,passes,successful_passes,key_passes,tackles,interceptions,saves,yellow_cards,red_cards,official_rating,source,is_deleted)
SELECT 36800000000000000+ROW_NUMBER() OVER (ORDER BY a.team_id,a.player_id),a.match_id,a.team_id,a.player_id,
       a.end_minute-a.start_minute,
       COALESCE(SUM(e.event_type='GOAL' AND e.player_id=a.player_id),0),COALESCE(SUM(e.event_type='GOAL' AND e.assist_player_id=a.player_id),0),
       2+COALESCE(SUM(e.event_type='GOAL' AND e.player_id=a.player_id),0)+MOD(a.player_id,3),1+COALESCE(SUM(e.event_type='GOAL' AND e.player_id=a.player_id),0),
       45+MOD(a.player_id,20),40+MOD(a.player_id,20),1+MOD(a.player_id,3),2+MOD(a.player_id,4),1+MOD(a.player_id,3),IF(a.position='GOALKEEPER',3+MOD(a.player_id,2),0),
       COALESCE(SUM(e.event_type='YELLOW_CARD' AND e.player_id=a.player_id),0),COALESCE(SUM(e.event_type='RED_CARD' AND e.player_id=a.player_id),0),
       LEAST(9.50,GREATEST(6.50,ROUND((6.80+COALESCE(SUM(e.event_type='GOAL' AND e.player_id=a.player_id),0)*0.70+COALESCE(SUM(e.event_type='GOAL' AND e.assist_player_id=a.player_id),0)*0.30+IF(a.started_flag=1,0.20,0))*2)/2)),'DEMO',0
FROM football_match_player_appearance a LEFT JOIN match_event e ON e.match_id=a.match_id AND e.status='ACTIVE' AND e.is_deleted=0
WHERE a.match_id=15000000000000060 AND a.appeared_flag=1 AND a.status='ACTIVE' AND a.is_deleted=0
  AND NOT EXISTS (SELECT 1 FROM football_match_player_stat x WHERE x.match_id=a.match_id AND x.player_id=a.player_id)
GROUP BY a.match_id,a.team_id,a.player_id,a.end_minute,a.start_minute,a.position,a.started_flag;

INSERT INTO football_match_team_stat(id,match_id,team_id,possession,shots,shots_on_target,corners,fouls,offsides,yellow_cards,red_cards,passes,successful_passes,pass_accuracy,saves,expected_goals,source,is_deleted)
SELECT 36700000000000000+ROW_NUMBER() OVER (ORDER BY p.team_id),p.match_id,p.team_id,CASE WHEN p.team_id=13000000000000012 THEN 56.00 ELSE 44.00 END,SUM(p.shots),SUM(p.shots_on_target),CASE WHEN p.team_id=13000000000000012 THEN 6 ELSE 4 END,
       CASE WHEN p.team_id=13000000000000012 THEN 10 ELSE 13 END,CASE WHEN p.team_id=13000000000000012 THEN 2 ELSE 1 END,SUM(p.yellow_cards),SUM(p.red_cards),SUM(p.passes),SUM(p.successful_passes),ROUND(SUM(p.successful_passes)*100.0/SUM(p.passes),2),SUM(p.saves),ROUND(SUM(p.goals)*0.70+SUM(p.shots_on_target)*0.08,2),'DEMO',0
FROM football_match_player_stat p WHERE p.match_id=15000000000000060 AND p.is_deleted=0
GROUP BY p.match_id,p.team_id
HAVING NOT EXISTS (SELECT 1 FROM football_match_team_stat x WHERE x.match_id=p.match_id AND x.team_id=p.team_id);

INSERT INTO football_user_player_rating(id,match_id,player_id,user_id,rating,status,is_deleted)
SELECT 37000000000000000+ROW_NUMBER() OVER (ORDER BY p.player_id,u.id),p.match_id,p.player_id,u.id,
       LEAST(10.0,GREATEST(1.0,ROUND((p.official_rating+(MOD(u.id,3)-1)*0.5)*2)/2)),'ACTIVE',0
FROM football_match_player_stat p JOIN sys_user u ON u.id IN(11000000000000001,11000000000000002,11000000000000003)
WHERE p.match_id=15000000000000060 AND p.is_deleted=0
  AND NOT EXISTS (SELECT 1 FROM football_user_player_rating x WHERE x.match_id=p.match_id AND x.player_id=p.player_id AND x.user_id=u.id);

INSERT INTO football_standing(id,league_id,season_id,stage_id,group_code,team_id,rank_no,played,won,drawn,lost,goals_for,goals_against,goal_difference,points,deduction_points,form_text,source,is_deleted)
VALUES
  (37100000000000001,12000000000000004,20000000000000008,21000000000000008,'',13000000000000012,9,20,8,4,8,30,28,2,28,0,'WDLWW','DEMO',0),
  (37100000000000002,12000000000000004,20000000000000008,21000000000000008,'',13000000000000011,10,20,7,5,8,27,29,-2,26,0,'DLWWL','DEMO',0)
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO football_team_competition_stat(id,league_id,season_id,stage_id,team_id,played,goals_for,goals_against,assists,yellow_cards,red_cards,shots,shots_on_target,corners,fouls,clean_sheets,avg_rating,source,is_deleted)
VALUES
  (37200000000000001,12000000000000004,20000000000000008,21000000000000008,13000000000000012,20,30,28,18,31,1,190,86,104,210,7,7.12,'DEMO',0),
  (37200000000000002,12000000000000004,20000000000000008,21000000000000008,13000000000000011,20,27,29,16,34,1,176,74,96,226,6,7.04,'DEMO',0)
ON DUPLICATE KEY UPDATE id=id;

COMMIT;

SELECT 'P1_M2_new_players' metric,COUNT(*) value FROM football_player WHERE id>=36000000000000001 AND id<36100000000000001 AND remark LIKE 'P1_M2_DEMO_ROSTER:%';
SELECT 'P1_M2_roster_rows' metric,COUNT(*) value FROM football_team_season_player WHERE id>=36200000000000001 AND id<36300000000000001 AND source='DEMO' AND is_deleted=0;
SELECT 'P1_M2_appearances' metric,COUNT(*) value FROM football_match_player_appearance WHERE id>=36600000000000001 AND id<36700000000000001 AND is_deleted=0;
SELECT 'P1_M2_player_stats' metric,COUNT(*) value FROM football_match_player_stat WHERE id>=36800000000000001 AND id<36900000000000001 AND is_deleted=0;
SELECT 'P1_M2_events' metric,COUNT(*) value FROM match_event WHERE id>=36900000000000001 AND id<37000000000000001 AND is_deleted=0;
