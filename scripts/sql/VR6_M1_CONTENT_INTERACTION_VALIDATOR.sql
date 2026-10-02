USE south_stand;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS vr6_m1_validate;
DELIMITER $$
CREATE PROCEDURE vr6_m1_validate()
BEGIN
  IF (SELECT COUNT(*) FROM content_media WHERE content_id=16000000000000201 AND status='ACTIVE' AND is_deleted=0) <> 3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR6 validation failed: target must have exactly 3 active media rows';
  END IF;
  IF (SELECT COUNT(*) FROM content_media WHERE id>=16100000000000601 AND id<16100000000000603 AND content_id=16000000000000201 AND media_type='IMAGE' AND media_url='/demo/p1-media/cover-stadium.png' AND status='ACTIVE' AND is_deleted=0 AND remark LIKE 'VR6-M1-media-%') <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR6 validation failed: VR6 media ownership or PNG contract is invalid';
  END IF;
  IF (SELECT COUNT(*) FROM comment WHERE id>=16400000000000601 AND id<16400000000000607 AND target_type='CONTENT' AND target_id=16000000000000201 AND parent_id=0 AND root_id=id AND status='ACTIVE' AND is_deleted=0 AND remark LIKE 'VR6-M1-comment-%') <> 6 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR6 validation failed: root comment coverage is invalid';
  END IF;
  IF (SELECT COUNT(*) FROM comment WHERE id>=16400000000000607 AND id<16400000000000615 AND target_type='CONTENT' AND target_id=16000000000000201 AND parent_id<>0 AND root_id<>0 AND status='ACTIVE' AND is_deleted=0 AND remark LIKE 'VR6-M1-comment-%') <> 8 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR6 validation failed: reply comment coverage is invalid';
  END IF;
  IF (SELECT COUNT(*) FROM (SELECT p.id FROM comment p JOIN comment r ON r.parent_id=p.id AND r.root_id=p.id WHERE p.id IN (16400000000000601,16400000000000602) GROUP BY p.id HAVING COUNT(*)>=3) roots_with_three_replies) <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR6 validation failed: two roots must have at least three replies';
  END IF;
  IF EXISTS (SELECT 1 FROM comment r LEFT JOIN comment p ON p.id=r.parent_id WHERE r.id>=16400000000000607 AND r.id<16400000000000615 AND (p.id IS NULL OR p.target_id<>r.target_id OR r.root_id<>p.root_id OR r.reply_to_user_id IS NULL)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR6 validation failed: reply parent/root/replyTo relation is invalid';
  END IF;
  IF EXISTS (SELECT 1 FROM comment p WHERE p.id>=16400000000000601 AND p.id<16400000000000607 AND p.reply_count<>(SELECT COUNT(*) FROM comment r WHERE r.parent_id=p.id AND r.status='ACTIVE' AND r.is_deleted=0)) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR6 validation failed: root reply_count mismatch';
  END IF;
  IF (SELECT comment_count FROM content WHERE id=16000000000000201) <> (SELECT COUNT(*) FROM comment WHERE target_type='CONTENT' AND target_id=16000000000000201 AND status='ACTIVE' AND is_deleted=0) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR6 validation failed: content comment_count mismatch';
  END IF;
  IF (SELECT COUNT(*) FROM content_media WHERE id=16100000000000201 AND content_id=16000000000000201 AND remark LIKE 'P1-M3-media-%') <> 1 OR
     (SELECT COUNT(*) FROM comment WHERE id IN (16400000000000201,16400000000000202) AND target_id=16000000000000201 AND remark LIKE 'P1-M3-comment-%') <> 2 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR6 validation failed: P1-M3 rows were not preserved';
  END IF;
  IF EXISTS (SELECT 1 FROM content_media WHERE id>=16100000000000601 AND id<16100000000000603 AND content_id<>16000000000000201) OR
     EXISTS (SELECT 1 FROM comment WHERE id>=16400000000000601 AND id<16400000000000615 AND target_id<>16000000000000201) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR6 validation failed: non-target leakage detected';
  END IF;
END$$
DELIMITER ;
CALL vr6_m1_validate();
DROP PROCEDURE vr6_m1_validate;

SELECT 'VR6_M1_target_media' metric,COUNT(*) value FROM content_media WHERE content_id=16000000000000201 AND status='ACTIVE' AND is_deleted=0;
SELECT 'VR6_M1_target_roots' metric,COUNT(*) value FROM comment WHERE target_type='CONTENT' AND target_id=16000000000000201 AND parent_id=0 AND status='ACTIVE' AND is_deleted=0;
SELECT 'VR6_M1_target_comments' metric,COUNT(*) value FROM comment WHERE target_type='CONTENT' AND target_id=16000000000000201 AND status='ACTIVE' AND is_deleted=0;
SELECT 'VR6_M1_content_comment_count' metric,comment_count value FROM content WHERE id=16000000000000201;
