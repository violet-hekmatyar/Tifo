USE south_stand;
SET NAMES utf8mb4;

-- Protective rollback: abort rather than remove a repurposed row or user work.
DROP PROCEDURE IF EXISTS vr14_r3_transfer_rollback_assert_safe;
DELIMITER $$
CREATE PROCEDURE vr14_r3_transfer_rollback_assert_safe()
BEGIN
  IF EXISTS (SELECT 1 FROM content WHERE id=16000000000002001
        AND (remark IS NULL OR remark<>'VR14_R3_TRANSFER_DEMO_CONTENT'
          OR content_type<>'POST' OR card_type<>'TRANSFER_BRIEF'
          OR COALESCE(JSON_UNQUOTE(JSON_EXTRACT(extra_json,'$.displayType')),'')<>'TRANSFER_BRIEF'))
     OR EXISTS (SELECT 1 FROM content_relation
        WHERE id BETWEEN 16300000000002001 AND 16300000000002003
          AND (remark IS NULL OR remark NOT LIKE 'VR14-R3-transfer-relation-%'
            OR content_id<>16000000000002001)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR14-R3 rollback refused: reserved rows changed ownership or shape';
  END IF;
  IF EXISTS (SELECT 1 FROM comment WHERE target_type='CONTENT' AND target_id=16000000000002001)
     OR EXISTS (SELECT 1 FROM like_record WHERE target_type='CONTENT' AND target_id=16000000000002001 AND status='ACTIVE')
     OR EXISTS (SELECT 1 FROM favorite_record WHERE target_type='CONTENT' AND target_id=16000000000002001 AND status='ACTIVE') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR14-R3 rollback refused: user interaction exists on demo content';
  END IF;
END$$
DELIMITER ;
CALL vr14_r3_transfer_rollback_assert_safe();
DROP PROCEDURE vr14_r3_transfer_rollback_assert_safe;

START TRANSACTION;
DELETE FROM content_relation
WHERE id BETWEEN 16300000000002001 AND 16300000000002003
  AND content_id=16000000000002001
  AND remark LIKE 'VR14-R3-transfer-relation-%';
DELETE FROM content
WHERE id=16000000000002001
  AND remark='VR14_R3_TRANSFER_DEMO_CONTENT'
  AND content_type='POST' AND card_type='TRANSFER_BRIEF'
  AND COALESCE(JSON_UNQUOTE(JSON_EXTRACT(extra_json,'$.displayType')),'')='TRANSFER_BRIEF';
COMMIT;

SELECT 'vr14_r3_transfer_remaining' AS metric,
       (SELECT COUNT(*) FROM content WHERE id=16000000000002001)
       +(SELECT COUNT(*) FROM content_relation WHERE id BETWEEN 16300000000002001 AND 16300000000002003) AS value;
