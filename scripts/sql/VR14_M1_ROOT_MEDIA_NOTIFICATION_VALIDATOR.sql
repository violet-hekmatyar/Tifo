USE south_stand;
SET NAMES utf8mb4;

-- Every target must point to a PNG path served by the local demo media root.
SELECT 'team_media_path_invalid' check_name, COUNT(*) invalid_count
FROM football_team
WHERE id IN (30001,30002,30003,30004,30005,30006,
             13000000000000010,13000000000000011,13000000000000012)
  AND (logo_url IS NULL OR logo_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'player_media_path_invalid' check_name, COUNT(*) invalid_count
FROM football_player
WHERE (id BETWEEN 40001 AND 40010 OR id IN
      (14000000000000001,14000000000000004,14000000000000010,
       14000000000000019,14000000000000028,14000000000000061,
       14000000000000067))
  AND (avatar_url IS NULL OR avatar_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'user_media_path_invalid' check_name, COUNT(*) invalid_count
FROM user_profile
WHERE user_id IN (10002,11000000000000001,11000000000000002,11000000000000003,
                  11000000000000004,11000000000000005,11000000000000006,
                  11000000000000007,11000000000000008,11000000000000009,
                  11000000000000010,11000000000000015,11000000000000017,
                  11000000000000022,11000000000000029)
  AND (avatar_url IS NULL OR avatar_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'legacy_content_media_path_invalid' check_name, COUNT(*) invalid_count
FROM content
WHERE id BETWEEN 20001 AND 20006
  AND (cover_url IS NULL OR cover_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'legacy_content_media_row_invalid' check_name, COUNT(*) invalid_count
FROM content_media
WHERE content_id IN (20001,20003,20004,20005)
  AND status='ACTIVE' AND is_deleted=0
  AND (media_url IS NULL OR media_url NOT LIKE '/demo/p1-media/%.png'
       OR thumbnail_url IS NULL OR thumbnail_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'home_demo_content_media_invalid' check_name, COUNT(*) invalid_count
FROM content
WHERE id IN (16000000000000007,16000000000000008,
             16000000000000009,16000000000000010)
  AND (cover_url IS NULL OR cover_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'home_demo_content_media_row_invalid' check_name, COUNT(*) invalid_count
FROM content_media
WHERE content_id IN (16000000000000007,16000000000000008,
                     16000000000000009,16000000000000010)
  AND media_type='IMAGE' AND status='ACTIVE' AND is_deleted=0
  AND (media_url IS NULL OR media_url NOT LIKE '/demo/p1-media/%.png'
       OR thumbnail_url IS NULL OR thumbnail_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'legacy_content_block_media_invalid' check_name, COUNT(*) invalid_count
FROM content_block
WHERE id=62502 AND content_id=20003 AND block_type='IMAGE'
  AND (media_url IS NULL OR media_url NOT LIKE '/demo/p1-media/%.png');

-- Team identity protection: no two fixed teams may share a logo URL.
SELECT 'team_identity_logo_duplicate' check_name,
       COUNT(*) - COUNT(DISTINCT logo_url) invalid_count
FROM football_team
WHERE id IN (30001,30002,30003,30004,30005,30006,
             13000000000000010,13000000000000011,13000000000000012);

SELECT 'data_demo_team_media_invalid' check_name, COUNT(*) invalid_count
FROM football_team
WHERE id IN (13000000000000001,13000000000000002,13000000000000003,
             13000000000000004,13000000000000005,13000000000000006,
             13000000000000007,13000000000000008,13000000000000009,
             13000000000000013,13000000000000014,13000000000000015,
             13000000000000016,13000000000000017,13000000000000018,
             13000000000000019,13000000000000020,13000000000000021,
             13000000000000022,13000000000000023,13000000000000024)
  AND (logo_url IS NULL OR logo_url NOT LIKE '/demo/p1-media/%.png');

SELECT 'data_demo_team_identity_duplicate' check_name,
       COUNT(*) - COUNT(DISTINCT logo_url) invalid_count
FROM football_team
WHERE id IN (13000000000000001,13000000000000002,13000000000000003,
             13000000000000004,13000000000000005,13000000000000006,
             13000000000000007,13000000000000008,13000000000000009,
             13000000000000013,13000000000000014,13000000000000015,
             13000000000000016,13000000000000017,13000000000000018,
             13000000000000019,13000000000000020,13000000000000021,
             13000000000000022,13000000000000023,13000000000000024);

SELECT 'data_demo_league_media_invalid' check_name, COUNT(*) invalid_count
FROM football_league
WHERE id IN (12000000000000001,12000000000000002,12000000000000003,
             12000000000000004,12000000000000005,12000000000000006,
             12000000000000007,12000000000000008)
  AND (logo_url IS NULL OR logo_url NOT LIKE '/demo/p1-media/%.png');

-- VR11 owns these rows. M1 validates, but does not overwrite, their read state.
SELECT 'vr11_notification_count_invalid' check_name,
       CASE WHEN COUNT(*)=8 THEN 0 ELSE 1 END invalid_count
FROM notification
WHERE id BETWEEN 17100000000000101 AND 17100000000000108
  AND recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%';

SELECT 'vr11_notification_unread_invalid' check_name,
       CASE WHEN SUM(read_flag=0)=5 THEN 0 ELSE 1 END invalid_count
FROM notification
WHERE id BETWEEN 17100000000000101 AND 17100000000000108
  AND recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%';

SELECT 'vr11_notification_read_invalid' check_name,
       CASE WHEN SUM(read_flag=1)=3 THEN 0 ELSE 1 END invalid_count
FROM notification
WHERE id BETWEEN 17100000000000101 AND 17100000000000108
  AND recipient_user_id=10002 AND dedup_key LIKE 'VR11:M1:%';

SELECT 'vr14_m1_target_summary' metric,
       'fixed-demo-media-and-vr11-notification-read-only-check' value;
