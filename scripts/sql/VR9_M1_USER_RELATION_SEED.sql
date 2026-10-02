-- VR9-M1 user relation seed.
-- Scope: user 10002 and existing DEMO users 11000000000000001..16 only.
-- No user, profile identity, password, onboarding, content or schema changes.
USE south_stand;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS vr9_m1_user_relation_seed;
DELIMITER $$
CREATE PROCEDURE vr9_m1_user_relation_seed()
BEGIN
  IF (SELECT COUNT(*) FROM sys_user
      WHERE id=10002 AND username='test_user' AND status='ACTIVE'
        AND is_deleted=0 AND onboarding_completed=1) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 conflict: target DEMO user 10002 is missing or changed';
  END IF;

  IF (SELECT COUNT(*) FROM user_profile
      WHERE user_id=10002 AND status='ACTIVE' AND is_deleted=0) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 conflict: target DEMO profile 10002 is missing or changed';
  END IF;

  IF (SELECT COUNT(*) FROM sys_user
      WHERE id IN (11000000000000001,11000000000000002,11000000000000003,11000000000000004,
                   11000000000000005,11000000000000006,11000000000000007,11000000000000008,
                   11000000000000009,11000000000000010,11000000000000011,11000000000000012,
                   11000000000000013,11000000000000014,11000000000000015,11000000000000016)
        AND username LIKE 'demo_user_%' AND status='ACTIVE' AND is_deleted=0) <> 16 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 conflict: one or more relation users are not existing DEMO users';
  END IF;

  IF EXISTS (SELECT 1 FROM follow_record
             WHERE id BETWEEN 18100000000090001 AND 18100000000090020
               AND (remark IS NULL OR remark NOT LIKE 'VR9-M1-relation-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 conflict: reserved relation ID is owned by non-VR9 data';
  END IF;

  IF EXISTS (SELECT 1 FROM follow_record
             WHERE follow_type='USER'
               AND ((user_id=10002 AND target_id IN
                    (11000000000000001,11000000000000002,11000000000000003,11000000000000004,
                     11000000000000005,11000000000000006,11000000000000007,11000000000000008,
                     11000000000000009,11000000000000010))
                 OR (user_id IN
                    (11000000000000001,11000000000000002,11000000000000009,11000000000000010,
                     11000000000000011,11000000000000012,11000000000000013,11000000000000014,
                     11000000000000015,11000000000000016) AND target_id=10002))
               AND (id NOT BETWEEN 18100000000090001 AND 18100000000090020
                    OR remark IS NULL OR remark NOT LIKE 'VR9-M1-relation-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-M1 conflict: relation natural key is owned by non-VR9 data';
  END IF;

  START TRANSACTION;
  INSERT INTO follow_record
    (id,user_id,follow_type,target_id,is_main,status,is_deleted,remark)
  VALUES
    (18100000000090001,10002,'USER',11000000000000001,0,'ACTIVE',0,'VR9-M1-relation-following-01'),
    (18100000000090002,10002,'USER',11000000000000002,0,'ACTIVE',0,'VR9-M1-relation-following-02'),
    (18100000000090003,10002,'USER',11000000000000003,0,'ACTIVE',0,'VR9-M1-relation-following-03'),
    (18100000000090004,10002,'USER',11000000000000004,0,'ACTIVE',0,'VR9-M1-relation-following-04'),
    (18100000000090005,10002,'USER',11000000000000005,0,'ACTIVE',0,'VR9-M1-relation-following-05'),
    (18100000000090006,10002,'USER',11000000000000006,0,'ACTIVE',0,'VR9-M1-relation-following-06'),
    (18100000000090007,10002,'USER',11000000000000007,0,'ACTIVE',0,'VR9-M1-relation-following-07'),
    (18100000000090008,10002,'USER',11000000000000008,0,'ACTIVE',0,'VR9-M1-relation-following-08'),
    (18100000000090009,10002,'USER',11000000000000009,0,'ACTIVE',0,'VR9-M1-relation-following-09'),
    (18100000000090010,10002,'USER',11000000000000010,0,'ACTIVE',0,'VR9-M1-relation-following-10'),
    (18100000000090011,11000000000000001,'USER',10002,0,'ACTIVE',0,'VR9-M1-relation-mutual-01'),
    (18100000000090012,11000000000000002,'USER',10002,0,'ACTIVE',0,'VR9-M1-relation-mutual-02'),
    (18100000000090013,11000000000000009,'USER',10002,0,'ACTIVE',0,'VR9-M1-relation-follower-09'),
    (18100000000090014,11000000000000010,'USER',10002,0,'ACTIVE',0,'VR9-M1-relation-follower-10'),
    (18100000000090015,11000000000000011,'USER',10002,0,'ACTIVE',0,'VR9-M1-relation-follower-11'),
    (18100000000090016,11000000000000012,'USER',10002,0,'ACTIVE',0,'VR9-M1-relation-follower-12'),
    (18100000000090017,11000000000000013,'USER',10002,0,'ACTIVE',0,'VR9-M1-relation-follower-13'),
    (18100000000090018,11000000000000014,'USER',10002,0,'ACTIVE',0,'VR9-M1-relation-follower-14'),
    (18100000000090019,11000000000000015,'USER',10002,0,'ACTIVE',0,'VR9-M1-relation-follower-15'),
    (18100000000090020,11000000000000016,'USER',10002,0,'ACTIVE',0,'VR9-M1-relation-follower-16')
  ON DUPLICATE KEY UPDATE id=id;

  UPDATE user_profile
  SET follower_count=(SELECT COUNT(*) FROM follow_record
                      WHERE follow_type='USER' AND target_id=10002
                        AND status='ACTIVE' AND is_deleted=0)
  WHERE user_id=10002 AND status='ACTIVE' AND is_deleted=0;
  COMMIT;
END$$
DELIMITER ;
CALL vr9_m1_user_relation_seed();
DROP PROCEDURE IF EXISTS vr9_m1_user_relation_seed;

SELECT 'VR9_M1_relation_rows' AS metric, COUNT(*) AS value
FROM follow_record
WHERE id BETWEEN 18100000000090001 AND 18100000000090020
  AND remark LIKE 'VR9-M1-relation-%';
SELECT 'VR9_M1_target_followings' AS metric, COUNT(*) AS value
FROM follow_record
WHERE user_id=10002 AND follow_type='USER' AND status='ACTIVE' AND is_deleted=0;
SELECT 'VR9_M1_target_followers' AS metric, COUNT(*) AS value
FROM follow_record
WHERE target_id=10002 AND follow_type='USER' AND status='ACTIVE' AND is_deleted=0;
