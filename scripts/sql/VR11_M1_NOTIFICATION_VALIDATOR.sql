USE south_stand;
SET NAMES utf8mb4;

SELECT check_name, invalid_count FROM (
  SELECT 'seed_count_not_8' check_name,
    ABS(COUNT(*)-8) invalid_count
  FROM notification
  WHERE id BETWEEN 17100000000000101 AND 17100000000000108
    AND recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%'
  UNION ALL SELECT 'reserved_foreign_rows', COUNT(*)
  FROM notification
  WHERE id BETWEEN 17100000000000101 AND 17100000000000108
    AND NOT (recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%')
  UNION ALL SELECT 'dedup_prefix_leak', COUNT(*)
  FROM notification
  WHERE dedup_key LIKE 'VR11:M1:%' AND recipient_user_id<>10002
  UNION ALL SELECT 'recipient_mismatch', COUNT(*)
  FROM notification
  WHERE id BETWEEN 17100000000000101 AND 17100000000000108
    AND recipient_user_id<>10002
  UNION ALL SELECT 'actor_missing', COUNT(*)
  FROM notification n LEFT JOIN sys_user u ON u.id=n.actor_user_id
  WHERE n.id BETWEEN 17100000000000101 AND 17100000000000108
    AND (u.id IS NULL OR u.status<>'ACTIVE' OR u.is_deleted<>0)
  UNION ALL SELECT 'target_content_invalid', COUNT(*)
  FROM notification n LEFT JOIN content c ON c.id=n.target_id
  WHERE n.id BETWEEN 17100000000000101 AND 17100000000000108
    AND (n.target_type<>'CONTENT' OR c.id IS NULL OR c.author_id<>10002
      OR c.status<>'PUBLISHED' OR c.is_deleted<>0)
  UNION ALL SELECT 'notification_type_invalid', COUNT(*)
  FROM notification
  WHERE id BETWEEN 17100000000000101 AND 17100000000000108
    AND (notification_type<>'CONTENT_LIKED'
      OR secondary_target_type IS NOT NULL OR secondary_target_id IS NOT NULL)
  UNION ALL SELECT 'actor_target_duplicate', COALESCE(SUM(duplicate_count - 1), 0)
  FROM (
    SELECT actor_user_id,target_id,COUNT(*) duplicate_count
    FROM notification
    WHERE id BETWEEN 17100000000000101 AND 17100000000000108
      AND recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%'
    GROUP BY actor_user_id,target_id
    HAVING COUNT(*)>1
  ) pairs
  UNION ALL SELECT 'cover_missing', COUNT(*)
  FROM notification n LEFT JOIN content c ON c.id=n.target_id
  WHERE n.id BETWEEN 17100000000000101 AND 17100000000000108
    AND (c.cover_url IS NULL OR c.cover_url='')
  UNION ALL SELECT 'duplicate_dedup', COUNT(*)
  FROM (SELECT dedup_key FROM notification GROUP BY dedup_key HAVING COUNT(*)>1) d
  WHERE d.dedup_key LIKE 'VR11:M1:%'
  UNION ALL SELECT 'read_state_invalid', COUNT(*)
  FROM notification
  WHERE id BETWEEN 17100000000000101 AND 17100000000000108
    AND ((read_flag=0 AND read_time IS NOT NULL) OR (read_flag=1 AND read_time IS NULL))
  UNION ALL SELECT 'unread_less_than_3',
    CASE WHEN SUM(read_flag=0)<3 THEN 1 ELSE 0 END
  FROM notification
  WHERE id BETWEEN 17100000000000101 AND 17100000000000108
    AND recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%'
  UNION ALL SELECT 'read_less_than_3',
    CASE WHEN SUM(read_flag=1)<3 THEN 1 ELSE 0 END
  FROM notification
  WHERE id BETWEEN 17100000000000101 AND 17100000000000108
    AND recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%'
) checks ORDER BY check_name;

SELECT 'VR11_M1_unread' metric,COUNT(*) value
FROM notification
WHERE id BETWEEN 17100000000000101 AND 17100000000000108
  AND recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%' AND read_flag=0;
SELECT 'VR11_M1_read' metric,COUNT(*) value
FROM notification
WHERE id BETWEEN 17100000000000101 AND 17100000000000108
  AND recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%' AND read_flag=1;
