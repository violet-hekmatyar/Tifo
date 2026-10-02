USE south_stand;
SET NAMES utf8mb4;

START TRANSACTION;

DELETE FROM content_block WHERE id BETWEEN 16200000000000901 AND 16200000000000912;
DELETE FROM content_media WHERE id BETWEEN 16100000000000901 AND 16100000000000912 AND remark LIKE 'VR9-R1-media-%';
DELETE FROM content WHERE id BETWEEN 16000000000000901 AND 16000000000000912 AND remark='VR9_R1_DEMO_CONTENT';
DELETE FROM follow_record WHERE id BETWEEN 18100000000090101 AND 18100000000090104 AND remark LIKE 'VR9-R1-entity-%';

UPDATE user_profile p
SET p.post_count=(SELECT COUNT(*) FROM content c WHERE c.author_id=p.user_id AND c.status='PUBLISHED' AND c.is_deleted=0),
    p.team_follow_count=(SELECT COUNT(*) FROM follow_record f WHERE f.user_id=p.user_id AND f.follow_type='TEAM' AND f.status='ACTIVE' AND f.is_deleted=0),
    p.player_follow_count=(SELECT COUNT(*) FROM follow_record f WHERE f.user_id=p.user_id AND f.follow_type='PLAYER' AND f.status='ACTIVE' AND f.is_deleted=0)
WHERE p.user_id IN (10002,11000000000000016) AND p.status='ACTIVE' AND p.is_deleted=0;

COMMIT;

SELECT 'VR9_R1_remaining_contents' check_name,COUNT(*) value FROM content WHERE id BETWEEN 16000000000000901 AND 16000000000000912 AND remark='VR9_R1_DEMO_CONTENT';
SELECT 'VR9_M1_relation_rows_preserved' check_name,COUNT(*) value FROM follow_record WHERE id BETWEEN 18100000000090001 AND 18100000000090020 AND remark LIKE 'VR9-M1-relation-%';
