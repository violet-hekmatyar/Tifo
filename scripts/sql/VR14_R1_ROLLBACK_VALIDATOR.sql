USE south_stand;
SET NAMES utf8mb4;

-- This validator runs immediately after the two VR14 rollbacks. It checks the
-- known pre-VR14 values for every field touched by the seeds; it never writes.
SELECT 'm1_team_core_rollback_invalid' check_name, COUNT(*) invalid_count
FROM football_team
WHERE id IN (30001,30002,30003,30004,30005,30006,13000000000000010)
  AND logo_url <> CASE id
    WHEN 30001 THEN '/uploads/team/barcelona.png'
    WHEN 30002 THEN '/uploads/team/real-madrid.png'
    WHEN 30003 THEN '/uploads/team/bayern.png'
    WHEN 30004 THEN '/uploads/team/man-city.png'
    WHEN 30005 THEN '/uploads/team/arsenal.png'
    WHEN 30006 THEN '/uploads/team/liverpool.png'
    WHEN 13000000000000010 THEN '/demo/teams/team-10.svg' END;

SELECT 'm1_team_selector_rollback_invalid' check_name, COUNT(*) invalid_count
FROM football_team
WHERE id IN (13000000000000001,13000000000000002,13000000000000003,
             13000000000000004,13000000000000005,13000000000000006,
             13000000000000007,13000000000000008,13000000000000009,
             13000000000000013,13000000000000014,13000000000000015,
             13000000000000016,13000000000000017,13000000000000018,
             13000000000000019,13000000000000020,13000000000000021,
             13000000000000022,13000000000000023,13000000000000024)
  AND logo_url <> CASE id
    WHEN 13000000000000001 THEN '/demo/teams/team-01.svg'
    WHEN 13000000000000002 THEN '/demo/teams/team-02.svg'
    WHEN 13000000000000003 THEN '/demo/teams/team-03.svg'
    WHEN 13000000000000004 THEN '/demo/teams/team-04.svg'
    WHEN 13000000000000005 THEN '/demo/teams/team-05.svg'
    WHEN 13000000000000006 THEN '/demo/teams/team-06.svg'
    WHEN 13000000000000007 THEN '/demo/teams/team-07.svg'
    WHEN 13000000000000008 THEN '/demo/teams/team-08.svg'
    WHEN 13000000000000009 THEN '/demo/teams/team-09.svg'
    WHEN 13000000000000013 THEN '/demo/teams/team-01.svg'
    WHEN 13000000000000014 THEN '/demo/teams/team-02.svg'
    WHEN 13000000000000015 THEN '/demo/teams/team-03.svg'
    WHEN 13000000000000016 THEN '/demo/teams/team-04.svg'
    WHEN 13000000000000017 THEN '/demo/teams/team-05.svg'
    WHEN 13000000000000018 THEN '/demo/teams/team-06.svg'
    WHEN 13000000000000019 THEN '/demo/teams/team-07.svg'
    WHEN 13000000000000020 THEN '/demo/teams/team-08.svg'
    WHEN 13000000000000021 THEN '/demo/teams/team-09.svg'
    WHEN 13000000000000022 THEN '/demo/teams/team-10.svg'
    WHEN 13000000000000023 THEN '/demo/teams/team-11.svg'
    WHEN 13000000000000024 THEN '/demo/teams/team-12.svg' END;

SELECT 'm1_league_rollback_invalid' check_name, COUNT(*) invalid_count
FROM football_league
WHERE id BETWEEN 12000000000000001 AND 12000000000000008
  AND logo_url <> CONCAT('/demo/teams/team-', LPAD(id - 12000000000000000, 2, '0'), '.svg');

SELECT 'm1_player_rollback_invalid' check_name, COUNT(*) invalid_count
FROM football_player
WHERE id IN (14000000000000001,14000000000000004,
             14000000000000010,14000000000000019,14000000000000028)
  AND avatar_url <> CASE id
    WHEN 14000000000000001 THEN '/demo/players/player-01.svg'
    WHEN 14000000000000004 THEN '/demo/players/player-04.svg'
    WHEN 14000000000000010 THEN '/demo/players/player-10.svg'
    WHEN 14000000000000019 THEN '/demo/players/player-07.svg'
    WHEN 14000000000000028 THEN '/demo/players/player-04.svg' END;

SELECT 'm1_user_rollback_invalid' check_name, COUNT(*) invalid_count
FROM user_profile
WHERE user_id IN (10002,11000000000000004,11000000000000005,
                  11000000000000006,11000000000000007,11000000000000008,
                  11000000000000009,11000000000000010,
                  11000000000000015,11000000000000017,
                  11000000000000022,11000000000000029)
  AND avatar_url <> CASE user_id
    WHEN 10002 THEN '/uploads/avatar/demo-fan.png'
    WHEN 11000000000000004 THEN '/demo/users/user-04.svg'
    WHEN 11000000000000005 THEN '/demo/users/user-05.svg'
    WHEN 11000000000000006 THEN '/demo/users/user-06.svg'
    WHEN 11000000000000007 THEN '/demo/users/user-07.svg'
    WHEN 11000000000000008 THEN '/demo/users/user-08.svg'
    WHEN 11000000000000009 THEN '/demo/users/user-09.svg'
    WHEN 11000000000000010 THEN '/demo/users/user-10.svg'
    WHEN 11000000000000015 THEN '/demo/users/user-03.svg'
    WHEN 11000000000000017 THEN '/demo/users/user-05.svg'
    WHEN 11000000000000022 THEN '/demo/users/user-10.svg'
    WHEN 11000000000000029 THEN '/demo/users/user-05.svg' END;

SELECT 'm1_content_cover_rollback_invalid' check_name, COUNT(*) invalid_count
FROM content
WHERE id BETWEEN 20001 AND 20006
  AND cover_url <> CASE id
    WHEN 20001 THEN '/uploads/content/demo-news-cover.jpg'
    WHEN 20002 THEN '/uploads/content/demo-post-1.jpg'
    WHEN 20003 THEN '/uploads/content/demo-article-cover.jpg'
    WHEN 20004 THEN '/uploads/content/demo-report-cover.jpg'
    WHEN 20005 THEN '/uploads/content/demo-report-2-cover.jpg'
    WHEN 20006 THEN '/uploads/content/demo-post-2.jpg' END;

SELECT 'm1_content_media_rollback_invalid' check_name, COUNT(*) invalid_count
FROM content_media
WHERE content_id IN (20001,20003,20004,20005)
  AND (media_url <> CASE content_id
        WHEN 20001 THEN '/uploads/content/demo-news-1.jpg'
        WHEN 20003 THEN '/uploads/content/demo-article-1.jpg'
        WHEN 20004 THEN '/uploads/content/demo-report-1.jpg'
        WHEN 20005 THEN '/uploads/content/demo-report-2.jpg' END
       OR thumbnail_url <> CASE content_id
        WHEN 20001 THEN '/uploads/content/demo-news-1-thumb.jpg'
        WHEN 20003 THEN '/uploads/content/demo-article-1-thumb.jpg'
        WHEN 20004 THEN '/uploads/content/demo-report-1-thumb.jpg'
        WHEN 20005 THEN '/uploads/content/demo-report-2-thumb.jpg' END);

SELECT 'm1_home_media_rollback_invalid' check_name, COUNT(*) invalid_count
FROM content_media
WHERE content_id IN (16000000000000007,16000000000000008,
                     16000000000000009,16000000000000010)
  AND (media_url <> CONCAT('/demo/contents/cover-', LPAD(id - 16100000000005000, 2, '0'), '.svg')
       OR thumbnail_url <> CONCAT('/demo/contents/cover-', LPAD(id - 16100000000005000, 2, '0'), '.svg'));

SELECT 'm1_block_media_rollback_invalid' check_name, COUNT(*) invalid_count
FROM content_block
WHERE id=62502 AND media_url <> '/uploads/content/demo-article-1.jpg';

SELECT 'm2_hot_score_rollback_invalid' check_name, COUNT(*) invalid_count
FROM content
WHERE (id BETWEEN 16000000000000001 AND 16000000000000120
       AND hot_score <> 300.00 - (id - 16000000000000001))
   OR (id BETWEEN 16000000000000201 AND 16000000000000210
       AND hot_score <> 340.00 - (id - 16000000000000201))
   OR (id BETWEEN 16000000000000901 AND 16000000000000912
       AND hot_score <> 218.00 - (id - 16000000000000901))
   OR (id=20001 AND hot_score<>98.50)
   OR (id=20002 AND hot_score<>31.00)
   OR (id=20003 AND hot_score<>65.00)
   OR (id=20004 AND hot_score<>120.00)
   OR (id=20005 AND hot_score<>104.00)
   OR (id=20006 AND hot_score<>24.00);
