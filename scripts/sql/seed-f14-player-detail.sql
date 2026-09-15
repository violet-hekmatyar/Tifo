USE south_stand;
SET NAMES utf8mb4;

START TRANSACTION;

INSERT INTO football_match_player_appearance
  (id,match_id,team_id,player_id,lineup_type,position,shirt_number,captain_flag,
   started_flag,appeared_flag,start_minute,end_minute,status,source,
   source_record_id,is_deleted)
SELECT
  (SELECT GREATEST(34000000000000001,COALESCE(MAX(id)+1,34000000000000001))
   FROM football_match_player_appearance),
  50004,30004,40007,'STARTER','FORWARD',9,0,1,1,0,90,'ACTIVE','DEMO',
  'f14-player-detail-50004-40007',0
WHERE EXISTS (
  SELECT 1 FROM match_info
  WHERE id=50004 AND away_team_id=30004 AND match_status='FINISHED'
    AND status='ACTIVE' AND is_deleted=0
)
AND EXISTS (
  SELECT 1 FROM team_player
  WHERE team_id=30004 AND player_id=40007
    AND status='ACTIVE' AND is_deleted=0
)
AND NOT EXISTS (
  SELECT 1 FROM football_match_player_appearance
  WHERE match_id=50004 AND player_id=40007
);

INSERT INTO football_match_player_stat
  (id,match_id,team_id,player_id,minutes,goals,assists,shots,shots_on_target,
   passes,successful_passes,key_passes,tackles,interceptions,saves,
   yellow_cards,red_cards,official_rating,source,source_updated_at,is_deleted)
SELECT
  (SELECT GREATEST(34010000000000001,COALESCE(MAX(id)+1,34010000000000001))
   FROM football_match_player_stat),
  50004,30004,40007,90,0,0,4,2,31,27,1,1,0,0,0,0,7.40,'DEMO',
  '2026-07-20 12:00:00',0
WHERE EXISTS (
  SELECT 1 FROM football_match_player_appearance
  WHERE match_id=50004 AND player_id=40007
    AND status='ACTIVE' AND is_deleted=0
)
AND NOT EXISTS (
  SELECT 1 FROM football_match_player_stat
  WHERE match_id=50004 AND player_id=40007 AND is_deleted=0
);

INSERT INTO content_relation
  (id,content_id,relation_type,relation_id,confidence,source_type,status,
   is_deleted,remark)
SELECT
  (SELECT COALESCE(MAX(id),0)+1 FROM content_relation),
  20005,'PLAYER',40007,1.0000,'MANUAL','ACTIVE',0,
  'F14 player detail demo relation'
WHERE EXISTS (
  SELECT 1 FROM content
  WHERE id=20005 AND status='PUBLISHED' AND is_deleted=0
)
AND EXISTS (
  SELECT 1 FROM football_player
  WHERE id=40007 AND status='ACTIVE' AND is_deleted=0
)
AND NOT EXISTS (
  SELECT 1 FROM content_relation
  WHERE content_id=20005 AND relation_type='PLAYER' AND relation_id=40007
    AND status='ACTIVE' AND is_deleted=0
);

COMMIT;

SELECT 'player_matches' AS target,COUNT(*) AS active_rows
FROM football_match_player_stat
WHERE player_id=40007 AND is_deleted=0
UNION ALL
SELECT 'player_contents',COUNT(*)
FROM content_relation
WHERE relation_type='PLAYER' AND relation_id=40007
  AND status='ACTIVE' AND is_deleted=0;
