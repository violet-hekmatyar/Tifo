USE south_stand;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS vr7_m1_rollback;
DELIMITER $$
CREATE PROCEDURE vr7_m1_rollback()
BEGIN
  IF EXISTS (SELECT 1 FROM content_relation r JOIN content_publish_subject s ON s.id=r.relation_id AND s.subject_type=r.relation_type WHERE s.id>=16500000000000701 AND s.id<16500000000000717 AND (r.remark IS NULL OR r.remark NOT LIKE 'VR7-M1-relation-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR7 rollback refused: non-VR7 content relation references a VR7 subject';
  END IF;
  DELETE FROM content_relation WHERE id>=16300000000000701 AND id<16300000000000713 AND remark LIKE 'VR7-M1-relation-%';
  DELETE FROM content_publish_subject WHERE id>=16500000000000701 AND id<16500000000000717 AND remark LIKE 'VR7-M1-subject-%';
END$$
DELIMITER ;
START TRANSACTION;
CALL vr7_m1_rollback();
COMMIT;
DROP PROCEDURE vr7_m1_rollback;

SELECT 'VR7_M1_rollback_remaining_subjects' check_name,COUNT(*) value FROM content_publish_subject WHERE id>=16500000000000701 AND id<16500000000000717 AND remark LIKE 'VR7-M1-subject-%';
SELECT 'VR7_M1_rollback_remaining_relations' check_name,COUNT(*) value FROM content_relation WHERE id>=16300000000000701 AND id<16300000000000713 AND remark LIKE 'VR7-M1-relation-%';
