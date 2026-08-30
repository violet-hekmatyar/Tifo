USE south_stand;
SET NAMES utf8mb4;

SELECT check_name, invalid_count FROM (
  SELECT 'recipient_missing' check_name, COUNT(*) invalid_count FROM notification n LEFT JOIN sys_user u ON u.id=n.recipient_user_id WHERE u.id IS NULL
  UNION ALL SELECT 'actor_missing', COUNT(*) FROM notification n LEFT JOIN sys_user u ON u.id=n.actor_user_id WHERE n.actor_user_id IS NOT NULL AND u.id IS NULL
  UNION ALL SELECT 'self_interaction', COUNT(*) FROM notification WHERE actor_user_id=recipient_user_id AND notification_type<>'SYSTEM'
  UNION ALL SELECT 'invalid_notification_type', COUNT(*) FROM notification WHERE notification_type NOT IN ('CONTENT_LIKED','CONTENT_COMMENTED','COMMENT_REPLIED','COMMENT_LIKED','USER_FOLLOWED','SYSTEM')
  UNION ALL SELECT 'invalid_target_type', COUNT(*) FROM notification WHERE target_type IS NOT NULL AND target_type NOT IN ('CONTENT','COMMENT','USER','SYSTEM')
  UNION ALL SELECT 'duplicate_dedup_key', COUNT(*) FROM (SELECT dedup_key FROM notification GROUP BY dedup_key HAVING COUNT(*)>1) d
  UNION ALL SELECT 'unread_with_read_time', COUNT(*) FROM notification WHERE read_flag=0 AND read_time IS NOT NULL
  UNION ALL SELECT 'read_without_read_time', COUNT(*) FROM notification WHERE read_flag=1 AND read_time IS NULL
) checks ORDER BY check_name;
