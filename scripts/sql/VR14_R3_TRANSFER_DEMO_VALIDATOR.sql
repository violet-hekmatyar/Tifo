USE south_stand;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS vr14_r3_transfer_validate;
DELIMITER $$
CREATE PROCEDURE vr14_r3_transfer_validate()
BEGIN
  IF (SELECT COUNT(*) FROM content WHERE id=16000000000002001
        AND remark='VR14_R3_TRANSFER_DEMO_CONTENT' AND content_type='POST'
        AND content_format='POST_FORMAT' AND card_type='TRANSFER_BRIEF'
        AND author_id=11000000000000001 AND source_type='USER'
        AND status='PUBLISHED' AND is_deleted=0
        AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(extra_json,'$.displayType')),'')='TRANSFER_BRIEF'
        AND JSON_UNQUOTE(JSON_EXTRACT(extra_json,'$.transferBrief.playerName'))='黄云帆'
        AND JSON_UNQUOTE(JSON_EXTRACT(extra_json,'$.transferBrief.fromTeamName'))='尤文图斯'
        AND JSON_UNQUOTE(JSON_EXTRACT(extra_json,'$.transferBrief.toTeamName'))='AC米兰')<>1
     OR (SELECT COUNT(*) FROM content_relation WHERE id BETWEEN 16300000000002001 AND 16300000000002003
        AND content_id=16000000000002001 AND status='ACTIVE' AND is_deleted=0
        AND remark LIKE 'VR14-R3-transfer-relation-%')<>3
     OR EXISTS (SELECT 1 FROM content_relation WHERE id BETWEEN 16300000000002001 AND 16300000000002003
        AND (content_id<>16000000000002001 OR remark NOT LIKE 'VR14-R3-transfer-relation-%'))
     OR NOT EXISTS (SELECT 1 FROM content_relation WHERE id=16300000000002001 AND relation_type='PLAYER' AND relation_id=14000000000000067 AND content_id=16000000000002001 AND status='ACTIVE' AND is_deleted=0)
     OR NOT EXISTS (SELECT 1 FROM content_relation WHERE id=16300000000002002 AND relation_type='TEAM' AND relation_id=13000000000000012 AND content_id=16000000000002001 AND status='ACTIVE' AND is_deleted=0)
     OR NOT EXISTS (SELECT 1 FROM content_relation WHERE id=16300000000002003 AND relation_type='TEAM' AND relation_id=13000000000000011 AND content_id=16000000000002001 AND status='ACTIVE' AND is_deleted=0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR14-R3 transfer validator failed';
  END IF;
END$$
DELIMITER ;
CALL vr14_r3_transfer_validate();
DROP PROCEDURE vr14_r3_transfer_validate;

SELECT 'vr14_r3_transfer_content' AS metric,COUNT(*) AS value
FROM content WHERE id=16000000000002001 AND remark='VR14_R3_TRANSFER_DEMO_CONTENT';
SELECT 'vr14_r3_owned_relations' AS metric,COUNT(*) AS value
FROM content_relation WHERE content_id=16000000000002001
  AND remark LIKE 'VR14-R3-transfer-relation-%' AND status='ACTIVE' AND is_deleted=0;
