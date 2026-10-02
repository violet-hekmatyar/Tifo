USE south_stand;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS vr7_m1_validate;
DELIMITER $$
CREATE PROCEDURE vr7_m1_validate()
BEGIN
  IF (SELECT COUNT(*) FROM content_publish_subject WHERE id>=16500000000000701 AND id<16500000000000717 AND remark LIKE 'VR7-M1-subject-%' AND status='ACTIVE' AND is_deleted=0) <> 12 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR7 validation failed: subject coverage is not 12';
  END IF;
  IF (SELECT COUNT(*) FROM content_publish_subject WHERE id>=16500000000000701 AND id<16500000000000707 AND subject_type='TOPIC') <> 6 OR
     (SELECT COUNT(*) FROM content_publish_subject WHERE id>=16500000000000711 AND id<16500000000000717 AND subject_type='HOT_EVENT' AND cover_url IS NOT NULL) <> 6 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR7 validation failed: subject types or hot event media are invalid';
  END IF;
  IF EXISTS (SELECT 1 FROM content_relation r LEFT JOIN content_publish_subject s ON s.id=r.relation_id AND s.subject_type=r.relation_type AND s.status='ACTIVE' AND s.is_deleted=0 WHERE r.id>=16300000000000701 AND r.id<16300000000000713 AND (s.id IS NULL OR r.status<>'ACTIVE' OR r.is_deleted<>0)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR7 validation failed: relation target is invalid';
  END IF;
  IF EXISTS (SELECT 1 FROM (SELECT content_id,relation_type,relation_id,COUNT(*) n FROM content_relation WHERE status='ACTIVE' AND is_deleted=0 GROUP BY content_id,relation_type,relation_id HAVING COUNT(*)>1) x JOIN content_relation r ON r.content_id=x.content_id AND r.relation_type=x.relation_type AND r.relation_id=x.relation_id AND r.id>=16300000000000701 AND r.id<16300000000000713) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR7 validation failed: duplicate active relation';
  END IF;
  IF EXISTS (SELECT 1 FROM content_publish_subject WHERE id>=16500000000000701 AND id<16500000000000717 AND (remark IS NULL OR remark NOT LIKE 'VR7-M1-subject-%')) OR
     EXISTS (SELECT 1 FROM content_relation WHERE id>=16300000000000701 AND id<16300000000000713 AND (remark IS NULL OR remark NOT LIKE 'VR7-M1-relation-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR7 validation failed: non-VR7 ownership leakage';
  END IF;
END$$
DELIMITER ;
CALL vr7_m1_validate();
DROP PROCEDURE vr7_m1_validate;

SELECT subject_type,COUNT(*) value FROM content_publish_subject WHERE id>=16500000000000701 AND id<16500000000000717 AND status='ACTIVE' AND is_deleted=0 GROUP BY subject_type ORDER BY subject_type;
SELECT s.subject_type,s.id,s.name,COUNT(DISTINCT c.id) discussion_count
FROM content_publish_subject s
LEFT JOIN content_relation r ON r.relation_type=s.subject_type AND r.relation_id=s.id AND r.status='ACTIVE' AND r.is_deleted=0
LEFT JOIN content c ON c.id=r.content_id AND c.status IN ('ACTIVE','PUBLISHED') AND c.is_deleted=0
WHERE s.id>=16500000000000701 AND s.id<16500000000000717
GROUP BY s.subject_type,s.id,s.name ORDER BY s.subject_type,s.id;
