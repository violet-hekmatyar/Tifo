USE south_stand;
SET NAMES utf8mb4;

-- Adds one explicitly fictional R3 transfer display card. It is a normal
-- DEMO-owned POST with additive display metadata and relations to existing
-- P1 football objects; no existing content or R2 feed scores are changed.
DROP PROCEDURE IF EXISTS vr14_r3_transfer_assert_safe;
DELIMITER $$
CREATE PROCEDURE vr14_r3_transfer_assert_safe()
BEGIN
  IF EXISTS (
    SELECT 1 FROM content
    WHERE id=16000000000002001
      AND (remark IS NULL OR remark<>'VR14_R3_TRANSFER_DEMO_CONTENT'
        OR content_type<>'POST' OR card_type<>'TRANSFER_BRIEF'
        OR author_id<>11000000000000001 OR status<>'PUBLISHED' OR is_deleted<>0
        OR COALESCE(JSON_UNQUOTE(JSON_EXTRACT(extra_json,'$.displayType')),'')<>'TRANSFER_BRIEF')
  ) OR EXISTS (
    SELECT 1 FROM content_relation
    WHERE id BETWEEN 16300000000002001 AND 16300000000002003
      AND (remark IS NULL OR remark NOT LIKE 'VR14-R3-transfer-relation-%'
        OR content_id<>16000000000002001)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR14-R3 conflict: reserved demo identity is occupied or drifted';
  END IF;

  IF (SELECT COUNT(*) FROM football_team WHERE id IN (13000000000000011,13000000000000012)
        AND status='ACTIVE' AND is_deleted=0)<>2
     OR NOT EXISTS (SELECT 1 FROM football_player WHERE id=14000000000000067 AND status='ACTIVE' AND is_deleted=0)
     OR NOT EXISTS (SELECT 1 FROM user_profile WHERE user_id=11000000000000001 AND status='ACTIVE' AND is_deleted=0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR14-R3 dependencies missing; seed was not applied';
  END IF;
END$$
DELIMITER ;
CALL vr14_r3_transfer_assert_safe();
DROP PROCEDURE vr14_r3_transfer_assert_safe;

START TRANSACTION;
INSERT INTO content(
  id,content_type,content_format,card_type,title,summary,body,cover_url,
  author_id,source_type,source_name,is_official,view_count,like_count,
  comment_count,favorite_count,hot_score,publish_time,status,is_deleted,
  extra_json,remark
)
VALUES(
  16000000000002001,'POST','POST_FORMAT','TRANSFER_BRIEF',
  '演示转会快讯｜非真实交易数据',
  '本卡为视觉演示，球员、转会方向及金额均不代表真实交易。',
  '用于 VR14 首页视觉验收的明确演示内容。请勿将本卡理解为真实转会消息。',
  '/demo/p1-media/cover-stadium.png',11000000000000001,'USER','VR14 R3 DEMO',0,
  0,0,0,0,180.00,'2026-09-30 12:00:00','PUBLISHED',0,
  JSON_OBJECT(
    'displayType','TRANSFER_BRIEF',
    'transferBrief',JSON_OBJECT(
      'playerId',14000000000000067,'playerName','黄云帆',
      'playerMeta','演示资料 · 非真实转会','playerAvatarUrl','/demo/p1-media/player-flagship.png',
      'fromTeamId',13000000000000012,'fromTeamName','尤文图斯','fromTeamLogoUrl','/demo/p1-media/team-crest-cobalt.png',
      'toTeamId',13000000000000011,'toTeamName','AC米兰','toTeamLogoUrl','/demo/p1-media/team-crest-crimson.png',
      'feeLabel','演示数据','durationLabel','演示数据'
    )
  ),
  'VR14_R3_TRANSFER_DEMO_CONTENT'
)
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO content_relation(
  id,content_id,relation_type,relation_id,confidence,source_type,status,is_deleted,remark
)
VALUES
  (16300000000002001,16000000000002001,'PLAYER',14000000000000067,1.0000,'MANUAL','ACTIVE',0,'VR14-R3-transfer-relation-player'),
  (16300000000002002,16000000000002001,'TEAM',13000000000000012,1.0000,'MANUAL','ACTIVE',0,'VR14-R3-transfer-relation-from-team'),
  (16300000000002003,16000000000002001,'TEAM',13000000000000011,1.0000,'MANUAL','ACTIVE',0,'VR14-R3-transfer-relation-to-team')
ON DUPLICATE KEY UPDATE id=id;
COMMIT;
