USE south_stand;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS vr11_m1_assert_rollback_safe;
DELIMITER $$
CREATE PROCEDURE vr11_m1_assert_rollback_safe()
BEGIN
  IF EXISTS (
    SELECT 1 FROM notification
    WHERE id BETWEEN 17100000000000101 AND 17100000000000108
      AND NOT (recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%')
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR11 rollback stopped: reserved notification range contains non-VR11 data';
  END IF;
  IF EXISTS (
    SELECT 1 FROM notification
    WHERE dedup_key LIKE 'VR11:M1:%'
      AND NOT (id BETWEEN 17100000000000101 AND 17100000000000108 AND recipient_user_id=10002)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR11 rollback stopped: VR11 dedup prefix is attached to non-demo data';
  END IF;
END$$
DELIMITER ;
CALL vr11_m1_assert_rollback_safe();
DROP PROCEDURE vr11_m1_assert_rollback_safe;

START TRANSACTION;
DELETE FROM notification
WHERE id BETWEEN 17100000000000101 AND 17100000000000108
  AND recipient_user_id=10002
  AND dedup_key LIKE 'VR11:M1:%';
COMMIT;

SELECT 'VR11_M1_remaining' metric,COUNT(*) value
FROM notification
WHERE id BETWEEN 17100000000000101 AND 17100000000000108
   OR dedup_key LIKE 'VR11:M1:%';
