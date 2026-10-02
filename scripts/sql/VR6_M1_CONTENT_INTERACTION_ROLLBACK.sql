USE south_stand;
SET NAMES utf8mb4;

START TRANSACTION;
DELETE FROM comment
WHERE id>=16400000000000601 AND id<16400000000000615
  AND target_id=16000000000000201
  AND remark LIKE 'VR6-M1-comment-%';
DELETE FROM content_media
WHERE id>=16100000000000601 AND id<16100000000000603
  AND content_id=16000000000000201
  AND remark LIKE 'VR6-M1-media-%';
UPDATE content c
SET c.comment_count=(
  SELECT COUNT(*) FROM comment x
  WHERE x.target_type='CONTENT'
    AND x.target_id=c.id
    AND x.status='ACTIVE'
    AND x.is_deleted=0
)
WHERE c.id=16000000000000201;
COMMIT;

SELECT 'VR6_M1_rollback_remaining' check_name,COUNT(*) value FROM (
  SELECT id FROM content_media WHERE id>=16100000000000601 AND id<16100000000000603
  UNION ALL
  SELECT id FROM comment WHERE id>=16400000000000601 AND id<16400000000000615
) x;
SELECT 'VR6_M1_original_media_preserved' check_name,COUNT(*) value
FROM content_media WHERE id=16100000000000201 AND content_id=16000000000000201;
SELECT 'VR6_M1_original_comments_preserved' check_name,COUNT(*) value
FROM comment WHERE id IN (16400000000000201,16400000000000202) AND target_id=16000000000000201;
