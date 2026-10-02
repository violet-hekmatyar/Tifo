-- VR9-M1 relation validator. Any SIGNAL aborts validation.
USE south_stand;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS vr9_m1_user_relation_validate;
DELIMITER $$
CREATE PROCEDURE vr9_m1_user_relation_validate()
BEGIN
  IF (SELECT COUNT(*) FROM follow_record
      WHERE id BETWEEN 18100000000090001 AND 18100000000090020
        AND remark LIKE 'VR9-M1-relation-%' AND follow_type='USER'
        AND status='ACTIVE' AND is_deleted=0) <> 20 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 validation failed: relation coverage is not 20';
  END IF;

  IF (SELECT COUNT(*) FROM follow_record
      WHERE user_id=10002 AND follow_type='USER' AND status='ACTIVE' AND is_deleted=0) < 8
     OR (SELECT COUNT(*) FROM follow_record
         WHERE target_id=10002 AND follow_type='USER' AND status='ACTIVE' AND is_deleted=0) < 8 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 validation failed: both relation lists must contain at least 8 rows';
  END IF;

  IF EXISTS (SELECT 1 FROM follow_record
             WHERE id BETWEEN 18100000000090001 AND 18100000000090020
               AND (NOT ((user_id=10002 AND target_id IN
                          (11000000000000001,11000000000000002,11000000000000003,11000000000000004,
                           11000000000000005,11000000000000006,11000000000000007,11000000000000008,
                           11000000000000009,11000000000000010))
                     OR (target_id=10002 AND user_id IN
                          (11000000000000001,11000000000000002,11000000000000009,11000000000000010,
                           11000000000000011,11000000000000012,11000000000000013,11000000000000014,
                           11000000000000015,11000000000000016)))
                    OR remark IS NULL OR remark NOT LIKE 'VR9-M1-relation-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 validation failed: reserved rows leaked outside DEMO scope';
  END IF;

  IF EXISTS (SELECT 1 FROM follow_record
             WHERE id BETWEEN 18100000000090001 AND 18100000000090020
               AND (remark IS NULL OR remark NOT LIKE 'VR9-M1-relation-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 validation failed: reserved ID ownership changed';
  END IF;

  IF (SELECT follower_count FROM user_profile WHERE user_id=10002 AND is_deleted=0) <>
     (SELECT COUNT(*) FROM follow_record
      WHERE target_id=10002 AND follow_type='USER' AND status='ACTIVE' AND is_deleted=0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 validation failed: target follower count is stale';
  END IF;

  IF EXISTS (SELECT 1 FROM follow_record
             WHERE follow_type='USER' AND status='ACTIVE' AND is_deleted=0
             GROUP BY user_id,target_id HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 validation failed: duplicate active user relation';
  END IF;
END$$
DELIMITER ;
CALL vr9_m1_user_relation_validate();
DROP PROCEDURE IF EXISTS vr9_m1_user_relation_validate;

SELECT 'VR9_M1_reserved_rows' AS check_name, COUNT(*) AS value
FROM follow_record WHERE id BETWEEN 18100000000090001 AND 18100000000090020
  AND remark LIKE 'VR9-M1-relation-%';
SELECT 'VR9_M1_target_followings' AS check_name, COUNT(*) AS value
FROM follow_record WHERE user_id=10002 AND follow_type='USER' AND status='ACTIVE' AND is_deleted=0;
SELECT 'VR9_M1_target_followers' AS check_name, COUNT(*) AS value
FROM follow_record WHERE target_id=10002 AND follow_type='USER' AND status='ACTIVE' AND is_deleted=0;
SELECT 'VR9_M1_relation_semantics' AS check_name,
  SUM(CASE WHEN f.user_id=10002 AND reciprocal.id IS NOT NULL THEN 1 ELSE 0 END) AS mutual_following,
  SUM(CASE WHEN f.user_id=10002 AND reciprocal.id IS NULL THEN 1 ELSE 0 END) AS following_only,
  SUM(CASE WHEN f.user_id<>10002 AND reciprocal.id IS NULL THEN 1 ELSE 0 END) AS follower_only
FROM follow_record f
LEFT JOIN follow_record reciprocal
  ON reciprocal.user_id=f.target_id AND reciprocal.target_id=f.user_id
 AND reciprocal.follow_type='USER' AND reciprocal.status='ACTIVE' AND reciprocal.is_deleted=0
WHERE f.id BETWEEN 18100000000090001 AND 18100000000090020
  AND f.follow_type='USER' AND f.status='ACTIVE' AND f.is_deleted=0;
