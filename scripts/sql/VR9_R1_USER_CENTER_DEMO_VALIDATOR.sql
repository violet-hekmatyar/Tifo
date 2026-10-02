USE south_stand;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS vr9_r1_user_center_validate;
DELIMITER $$
CREATE PROCEDURE vr9_r1_user_center_validate()
BEGIN
  IF (SELECT COUNT(*) FROM content WHERE id BETWEEN 16000000000000901 AND 16000000000000906 AND author_id=10002 AND status='PUBLISHED' AND is_deleted=0 AND remark='VR9_R1_DEMO_CONTENT') <> 6
     OR (SELECT COUNT(*) FROM content WHERE id BETWEEN 16000000000000907 AND 16000000000000912 AND author_id=11000000000000016 AND status='PUBLISHED' AND is_deleted=0 AND remark='VR9_R1_DEMO_CONTENT') <> 6 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 validation failed: author content coverage is not 6 + 6';
  END IF;
  IF (SELECT COUNT(*) FROM content_media WHERE id BETWEEN 16100000000000901 AND 16100000000000912 AND status='ACTIVE' AND is_deleted=0 AND remark LIKE 'VR9-R1-media-%') <> 12
     OR EXISTS (SELECT 1 FROM content c LEFT JOIN content_media m ON m.content_id=c.id AND m.status='ACTIVE' AND m.is_deleted=0 WHERE c.id BETWEEN 16000000000000901 AND 16000000000000912 AND m.id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 validation failed: media coverage is incomplete';
  END IF;
  IF (SELECT COUNT(*) FROM content_block WHERE id BETWEEN 16200000000000901 AND 16200000000000912 AND content_id BETWEEN 16000000000000901 AND 16000000000000912 AND status='ACTIVE' AND is_deleted=0) <> 12
     OR EXISTS (SELECT 1 FROM content c LEFT JOIN content_block b ON b.content_id=c.id AND b.status='ACTIVE' AND b.is_deleted=0 WHERE c.id BETWEEN 16000000000000901 AND 16000000000000912 AND b.id IS NULL)
     OR EXISTS (SELECT 1 FROM content_block WHERE id BETWEEN 16200000000000901 AND 16200000000000912 AND content_id NOT BETWEEN 16000000000000901 AND 16000000000000912) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 validation failed: block ownership or coverage is incomplete';
  END IF;
  IF EXISTS (SELECT 1 FROM content_media WHERE id BETWEEN 16100000000000901 AND 16100000000000912 AND content_id NOT BETWEEN 16000000000000901 AND 16000000000000912)
     OR EXISTS (SELECT 1 FROM content_media WHERE id BETWEEN 16100000000000901 AND 16100000000000912 AND media_url NOT IN ('/demo/p1-media/cover-stadium.png','/demo/p1-media/cover-training.png','/demo/p1-media/cover-tactics.png')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 validation failed: media ownership or URL is outside the approved DEMO scope';
  END IF;
  IF EXISTS (SELECT 1 FROM content WHERE id BETWEEN 16000000000000901 AND 16000000000000912 AND cover_url NOT IN ('/demo/p1-media/cover-stadium.png','/demo/p1-media/cover-training.png','/demo/p1-media/cover-tactics.png')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 validation failed: cover is outside approved P1 media';
  END IF;
  IF (SELECT COUNT(*) FROM follow_record WHERE user_id=10002 AND follow_type='TEAM' AND status='ACTIVE' AND is_deleted=0) < 3
     OR (SELECT COUNT(*) FROM follow_record WHERE user_id=10002 AND follow_type='PLAYER' AND status='ACTIVE' AND is_deleted=0) < 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 validation failed: stand previews are below three objects';
  END IF;
  IF (SELECT COUNT(*) FROM follow_record WHERE id BETWEEN 18100000000090101 AND 18100000000090104 AND user_id=10002 AND status='ACTIVE' AND is_deleted=0 AND ((follow_type='TEAM' AND target_id IN (30002,30003)) OR (follow_type='PLAYER' AND target_id IN (40002,40003)))) <> 4 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 validation failed: entity follow ownership or target scope is invalid';
  END IF;
  IF EXISTS (SELECT 1 FROM follow_record WHERE id BETWEEN 18100000000090001 AND 18100000000090020 AND remark NOT LIKE 'VR9-M1-relation-%') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 validation failed: VR9-M1 relation ownership changed';
  END IF;
  IF EXISTS (SELECT 1 FROM content WHERE id BETWEEN 16000000000000901 AND 16000000000000912 AND author_id NOT IN (10002,11000000000000016)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 validation failed: non-DEMO author leakage';
  END IF;
END$$
DELIMITER ;
CALL vr9_r1_user_center_validate();
DROP PROCEDURE IF EXISTS vr9_r1_user_center_validate;

SELECT 'VR9_R1_reserved_contents' check_name,COUNT(*) value FROM content WHERE id BETWEEN 16000000000000901 AND 16000000000000912 AND remark='VR9_R1_DEMO_CONTENT';
SELECT 'VR9_R1_reserved_media' check_name,COUNT(*) value FROM content_media WHERE id BETWEEN 16100000000000901 AND 16100000000000912 AND remark LIKE 'VR9-R1-media-%';
SELECT 'VR9_R1_reserved_blocks' check_name,COUNT(*) value FROM content_block WHERE id BETWEEN 16200000000000901 AND 16200000000000912;
SELECT 'VR9_R1_m1_relation_rows' check_name,COUNT(*) value FROM follow_record WHERE id BETWEEN 18100000000090001 AND 18100000000090020 AND remark LIKE 'VR9-M1-relation-%';
