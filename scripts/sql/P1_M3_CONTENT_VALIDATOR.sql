USE south_stand;
SET NAMES utf8mb4;

SELECT 'content_count' check_name,COUNT(*) value FROM content WHERE id BETWEEN 16000000000000201 AND 16000000000000210 AND remark='P1_M3_DEMO_CONTENT' AND content_type IN('POST','ARTICLE') AND status='PUBLISHED' AND is_deleted=0;
SELECT 'content_invalid_author' check_name,COUNT(*) value FROM content c LEFT JOIN sys_user u ON u.id=c.author_id AND u.status='ACTIVE' AND u.is_deleted=0 WHERE c.id BETWEEN 16000000000000201 AND 16000000000000210 AND (u.id IS NULL OR c.author_id NOT BETWEEN 11000000000000001 AND 11000000000000010);
SELECT 'content_cover_not_png' check_name,COUNT(*) value FROM content WHERE id BETWEEN 16000000000000201 AND 16000000000000210 AND (cover_url IS NULL OR cover_url NOT LIKE '/demo/p1-media/%.png');
SELECT 'content_missing_blocks' check_name,COUNT(*) value FROM content c LEFT JOIN content_block b ON b.content_id=c.id AND b.status='ACTIVE' AND b.is_deleted=0 WHERE c.id BETWEEN 16000000000000201 AND 16000000000000210 GROUP BY c.id HAVING COUNT(b.id)<2;
SELECT 'content_media_invalid' check_name,COUNT(*) value FROM content_media WHERE content_id BETWEEN 16000000000000201 AND 16000000000000210 AND (media_type<>'IMAGE' OR media_url NOT LIKE '/demo/p1-media/%.png' OR status<>'ACTIVE' OR is_deleted<>0);
SELECT 'content_without_media' check_name,COUNT(*) value FROM content c LEFT JOIN content_media m ON m.content_id=c.id AND m.status='ACTIVE' AND m.is_deleted=0 WHERE c.id BETWEEN 16000000000000201 AND 16000000000000210 GROUP BY c.id HAVING COUNT(m.id)=0;

SELECT 'relation_missing_target' check_name,COUNT(*) value FROM content_relation r LEFT JOIN football_team t ON r.relation_type='TEAM' AND t.id=r.relation_id AND t.status='ACTIVE' AND t.is_deleted=0 LEFT JOIN football_player p ON r.relation_type='PLAYER' AND p.id=r.relation_id AND p.status='ACTIVE' AND p.is_deleted=0 LEFT JOIN match_info m ON r.relation_type='MATCH' AND m.id=r.relation_id AND m.status='ACTIVE' AND m.is_deleted=0 WHERE r.content_id BETWEEN 16000000000000201 AND 16000000000000210 AND r.status='ACTIVE' AND r.is_deleted=0 AND ((r.relation_type='TEAM' AND t.id IS NULL) OR (r.relation_type='PLAYER' AND p.id IS NULL) OR (r.relation_type='MATCH' AND m.id IS NULL));
SELECT 'relation_type_coverage' check_name,relation_type,COUNT(*) value FROM content_relation WHERE content_id BETWEEN 16000000000000201 AND 16000000000000210 AND status='ACTIVE' AND is_deleted=0 GROUP BY relation_type ORDER BY relation_type;
SELECT 'relation_coverage_errors' check_name,COUNT(*) value FROM (SELECT 'TEAM' relation_type UNION ALL SELECT 'PLAYER' UNION ALL SELECT 'MATCH') expected LEFT JOIN (SELECT relation_type,COUNT(*) cnt FROM content_relation WHERE content_id BETWEEN 16000000000000201 AND 16000000000000210 AND status='ACTIVE' AND is_deleted=0 GROUP BY relation_type) actual ON actual.relation_type=expected.relation_type WHERE COALESCE(actual.cnt,0)=0;

SELECT 'comment_count_mismatch' check_name,COUNT(*) value FROM content c LEFT JOIN (SELECT target_id,COUNT(*) cnt FROM comment WHERE target_type='CONTENT' AND status='ACTIVE' AND is_deleted=0 GROUP BY target_id) x ON x.target_id=c.id WHERE c.id BETWEEN 16000000000000201 AND 16000000000000210 AND c.comment_count<>COALESCE(x.cnt,0);
SELECT 'comment_invalid_target' check_name,COUNT(*) value FROM comment c LEFT JOIN content x ON x.id=c.target_id AND x.status='PUBLISHED' AND x.is_deleted=0 WHERE c.id BETWEEN 16400000000000201 AND 16400000000000212 AND c.target_type='CONTENT' AND (x.id IS NULL OR c.status<>'ACTIVE' OR c.is_deleted<>0);
SELECT 'comment_parent_errors' check_name,COUNT(*) value FROM comment c LEFT JOIN comment p ON p.id=c.parent_id AND p.status='ACTIVE' AND p.is_deleted=0 WHERE c.id BETWEEN 16400000000000201 AND 16400000000000212 AND c.parent_id<>0 AND (p.id IS NULL OR c.root_id<>p.root_id OR p.target_id<>c.target_id);
SELECT 'comment_reply_count_mismatch' check_name,COUNT(*) value FROM comment p LEFT JOIN (SELECT parent_id,COUNT(*) cnt FROM comment WHERE status='ACTIVE' AND is_deleted=0 AND parent_id<>0 GROUP BY parent_id) x ON x.parent_id=p.id WHERE p.id BETWEEN 16400000000000201 AND 16400000000000212 AND p.reply_count<>COALESCE(x.cnt,0);

SELECT 'new_content_missing_from_feed_candidates' check_name,COUNT(*) value FROM content WHERE id BETWEEN 16000000000000201 AND 16000000000000210 AND status<>'PUBLISHED';
SELECT 'non_demo_leakage' check_name,COUNT(*) value FROM (
  SELECT id FROM content WHERE id BETWEEN 16000000000000201 AND 16000000000000210 AND (remark IS NULL OR remark<>'P1_M3_DEMO_CONTENT')
  UNION ALL SELECT id FROM content_media WHERE id BETWEEN 16100000000000201 AND 16100000000000210 AND (remark IS NULL OR remark NOT LIKE 'P1-M3-media-%')
  UNION ALL SELECT id FROM content_block WHERE id BETWEEN 16200000000000201 AND 16200000000000220 AND content_id NOT BETWEEN 16000000000000201 AND 16000000000000210
  UNION ALL SELECT id FROM content_relation WHERE id BETWEEN 16300000000000201 AND 16300000000000215 AND (remark IS NULL OR remark NOT LIKE 'P1-M3-relation-%')
  UNION ALL SELECT id FROM comment WHERE id BETWEEN 16400000000000201 AND 16400000000000212 AND (remark IS NULL OR remark NOT LIKE 'P1-M3-comment-%')
) x;

SELECT 'content_comment_summary' metric,target_id content_id,COUNT(*) comments FROM comment WHERE target_type='CONTENT' AND target_id BETWEEN 16000000000000201 AND 16000000000000210 AND status='ACTIVE' AND is_deleted=0 GROUP BY target_id ORDER BY target_id;
