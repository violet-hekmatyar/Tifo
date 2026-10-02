-- VR9-M1 protected rollback. Only VR9-owned relation rows are removed.
USE south_stand;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS vr9_m1_user_relation_rollback;
DELIMITER $$
CREATE PROCEDURE vr9_m1_user_relation_rollback()
BEGIN
  IF EXISTS (SELECT 1 FROM follow_record
             WHERE id BETWEEN 18100000000090001 AND 18100000000090020
               AND (remark IS NULL OR remark NOT LIKE 'VR9-M1-relation-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9 rollback refused: reserved relation row is not VR9-owned';
  END IF;

  START TRANSACTION;
  DELETE FROM follow_record
  WHERE id BETWEEN 18100000000090001 AND 18100000000090020
    AND remark LIKE 'VR9-M1-relation-%';

  UPDATE user_profile
  SET follower_count=(SELECT COUNT(*) FROM follow_record
                      WHERE follow_type='USER' AND target_id=10002
                        AND status='ACTIVE' AND is_deleted=0)
  WHERE user_id=10002 AND status='ACTIVE' AND is_deleted=0;
  COMMIT;
END$$
DELIMITER ;
CALL vr9_m1_user_relation_rollback();
DROP PROCEDURE IF EXISTS vr9_m1_user_relation_rollback;

SELECT 'VR9_M1_remaining_relations' AS check_name, COUNT(*) AS value
FROM follow_record WHERE id BETWEEN 18100000000090001 AND 18100000000090020
  AND remark LIKE 'VR9-M1-relation-%';
