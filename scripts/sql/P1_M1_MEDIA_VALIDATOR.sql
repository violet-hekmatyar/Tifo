-- P1-M1 targeted validator. Every anomaly_count must be 0.
SET NAMES utf8mb4;

SELECT 'team_logo_not_png' check_name, COUNT(*) anomaly_count
FROM football_team
WHERE id IN (13000000000000011, 13000000000000012)
  AND (logo_url IS NULL OR logo_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'player_avatar_not_png' check_name, COUNT(*) anomaly_count
FROM football_player
WHERE id IN (14000000000000061, 14000000000000067)
  AND (avatar_url IS NULL OR avatar_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'user_avatar_not_png' check_name, COUNT(*) anomaly_count
FROM user_profile
WHERE user_id IN (11000000000000001, 11000000000000002, 11000000000000003)
  AND (avatar_url IS NULL OR avatar_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'content_cover_not_png' check_name, COUNT(*) anomaly_count
FROM content
WHERE id BETWEEN 16000000000000001 AND 16000000000000006
  AND (cover_url IS NULL OR cover_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'content_media_not_png' check_name, COUNT(*) anomaly_count
FROM content_media
WHERE content_id BETWEEN 16000000000000001 AND 16000000000000006
  AND status = 'ACTIVE' AND is_deleted = 0
  AND (media_url IS NULL OR media_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'p1_media_leaked_outside_targets' check_name, COUNT(*) anomaly_count
FROM (
    SELECT id FROM football_team WHERE logo_url LIKE '/demo/p1-media/%'
      AND id NOT IN (13000000000000011, 13000000000000012)
    UNION ALL
    SELECT id FROM football_player WHERE avatar_url LIKE '/demo/p1-media/%'
      AND id NOT IN (14000000000000061, 14000000000000067)
    UNION ALL
    SELECT user_id FROM user_profile WHERE avatar_url LIKE '/demo/p1-media/%'
      AND user_id NOT IN (11000000000000001, 11000000000000002, 11000000000000003)
    UNION ALL
    SELECT id FROM content WHERE cover_url LIKE '/demo/p1-media/%'
      AND id NOT BETWEEN 16000000000000001 AND 16000000000000006
    UNION ALL
    SELECT content_id FROM content_media WHERE media_url LIKE '/demo/p1-media/%'
      AND content_id NOT BETWEEN 16000000000000001 AND 16000000000000006
) leaked;

SELECT 'p1_m1_media_targets' metric, 'teams=2 players=2 users=3 contents=6' value;
SELECT id, team_name, logo_url FROM football_team
WHERE id IN (13000000000000011, 13000000000000012) ORDER BY id;
SELECT id, player_name, avatar_url FROM football_player
WHERE id IN (14000000000000061, 14000000000000067) ORDER BY id;
SELECT user_id, nickname, avatar_url FROM user_profile
WHERE user_id IN (11000000000000001, 11000000000000002, 11000000000000003) ORDER BY user_id;
SELECT id, cover_url FROM content
WHERE id BETWEEN 16000000000000001 AND 16000000000000006 ORDER BY id;
