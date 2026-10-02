USE south_stand;
SET NAMES utf8mb4;
START TRANSACTION;

-- Rollback only values written by VR14-M1. Rows changed by a later workflow do
-- not match these predicates and are left untouched.
UPDATE football_team
SET logo_url = CASE id
    WHEN 30001 THEN '/uploads/team/barcelona.png'
    WHEN 30002 THEN '/uploads/team/real-madrid.png'
    WHEN 30003 THEN '/uploads/team/bayern.png'
    WHEN 30004 THEN '/uploads/team/man-city.png'
    WHEN 30005 THEN '/uploads/team/arsenal.png'
    WHEN 30006 THEN '/uploads/team/liverpool.png'
    WHEN 13000000000000010 THEN '/demo/teams/team-10.svg'
    ELSE logo_url END
WHERE id IN (30001,30002,30003,30004,30005,30006,13000000000000010)
  AND logo_url LIKE '/demo/p1-media/team-%.png';

UPDATE football_team
SET logo_url = CASE id
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
    WHEN 13000000000000024 THEN '/demo/teams/team-12.svg'
    ELSE logo_url END
WHERE id IN (13000000000000001,13000000000000002,13000000000000003,
             13000000000000004,13000000000000005,13000000000000006,
             13000000000000007,13000000000000008,13000000000000009,
             13000000000000013,13000000000000014,13000000000000015,
             13000000000000016,13000000000000017,13000000000000018,
             13000000000000019,13000000000000020,13000000000000021,
             13000000000000022,13000000000000023,13000000000000024)
  AND logo_url LIKE '/demo/p1-media/team-demo-%.png';

UPDATE football_league
SET logo_url = CASE id
    WHEN 12000000000000001 THEN '/demo/teams/team-01.svg'
    WHEN 12000000000000002 THEN '/demo/teams/team-02.svg'
    WHEN 12000000000000003 THEN '/demo/teams/team-03.svg'
    WHEN 12000000000000004 THEN '/demo/teams/team-04.svg'
    WHEN 12000000000000005 THEN '/demo/teams/team-05.svg'
    WHEN 12000000000000006 THEN '/demo/teams/team-06.svg'
    WHEN 12000000000000007 THEN '/demo/teams/team-07.svg'
    WHEN 12000000000000008 THEN '/demo/teams/team-08.svg'
    ELSE logo_url END
WHERE id IN (12000000000000001,12000000000000002,12000000000000003,
             12000000000000004,12000000000000005,12000000000000006,
             12000000000000007,12000000000000008)
  AND logo_url LIKE '/demo/p1-media/team-demo-%.png';

UPDATE football_player
SET avatar_url = CASE
    WHEN id BETWEEN 40001 AND 40010 THEN CONCAT('/uploads/player/',
      CASE id
        WHEN 40001 THEN 'lewandowski'
        WHEN 40002 THEN 'pedri'
        WHEN 40003 THEN 'vinicius'
        WHEN 40004 THEN 'bellingham'
        WHEN 40005 THEN 'kane'
        WHEN 40006 THEN 'musiala'
        WHEN 40007 THEN 'haaland'
        WHEN 40008 THEN 'saka'
        WHEN 40009 THEN 'salah'
        WHEN 40010 THEN 'alisson' END, '.png')
    ELSE avatar_url END
WHERE id BETWEEN 40001 AND 40010
  AND avatar_url='/demo/p1-media/player-flagship.png';

UPDATE football_player
SET avatar_url = CASE id
    WHEN 14000000000000001 THEN '/demo/players/player-01.svg'
    WHEN 14000000000000010 THEN '/demo/players/player-10.svg'
    WHEN 14000000000000019 THEN '/demo/players/player-07.svg'
    WHEN 14000000000000028 THEN '/demo/players/player-04.svg'
    WHEN 14000000000000004 THEN '/demo/players/player-04.svg'
    ELSE avatar_url END
WHERE id IN (14000000000000001,14000000000000004,
             14000000000000010,14000000000000019,14000000000000028)
  AND avatar_url='/demo/p1-media/player-flagship.png';

UPDATE user_profile
SET avatar_url = CASE user_id
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
    WHEN 11000000000000029 THEN '/demo/users/user-05.svg'
    ELSE avatar_url END
WHERE user_id IN (10002,11000000000000004,11000000000000005,
                  11000000000000006,11000000000000007,11000000000000008,
                  11000000000000009,11000000000000010,
                  11000000000000015,11000000000000017,11000000000000022,
                  11000000000000029)
  AND avatar_url='/demo/p1-media/user-demo.png';

UPDATE content
SET cover_url = CASE id
    WHEN 20001 THEN '/uploads/content/demo-news-cover.jpg'
    WHEN 20002 THEN '/uploads/content/demo-post-1.jpg'
    WHEN 20003 THEN '/uploads/content/demo-article-cover.jpg'
    WHEN 20004 THEN '/uploads/content/demo-report-cover.jpg'
    WHEN 20005 THEN '/uploads/content/demo-report-2-cover.jpg'
    WHEN 20006 THEN '/uploads/content/demo-post-2.jpg'
    ELSE cover_url END
WHERE id BETWEEN 20001 AND 20006
  AND cover_url LIKE '/demo/p1-media/%.png';

UPDATE content_media
SET media_url = CASE content_id
        WHEN 20001 THEN '/uploads/content/demo-news-1.jpg'
        WHEN 20003 THEN '/uploads/content/demo-article-1.jpg'
        WHEN 20004 THEN '/uploads/content/demo-report-1.jpg'
        WHEN 20005 THEN '/uploads/content/demo-report-2.jpg'
        ELSE media_url END,
    thumbnail_url = CASE content_id
        WHEN 20001 THEN '/uploads/content/demo-news-1-thumb.jpg'
        WHEN 20003 THEN '/uploads/content/demo-article-1-thumb.jpg'
        WHEN 20004 THEN '/uploads/content/demo-report-1-thumb.jpg'
        WHEN 20005 THEN '/uploads/content/demo-report-2-thumb.jpg'
        ELSE thumbnail_url END
WHERE content_id IN (20001,20003,20004,20005)
  AND media_type='IMAGE'
  AND media_url LIKE '/demo/p1-media/%.png'
  AND thumbnail_url LIKE '/demo/p1-media/%.png';

UPDATE content_media
SET media_url = CASE content_id
        WHEN 16000000000000007 THEN '/demo/contents/cover-07.svg'
        WHEN 16000000000000008 THEN '/demo/contents/cover-08.svg'
        WHEN 16000000000000009 THEN '/demo/contents/cover-09.svg'
        WHEN 16000000000000010 THEN '/demo/contents/cover-10.svg'
        ELSE media_url END,
    thumbnail_url = CASE content_id
        WHEN 16000000000000007 THEN '/demo/contents/cover-07.svg'
        WHEN 16000000000000008 THEN '/demo/contents/cover-08.svg'
        WHEN 16000000000000009 THEN '/demo/contents/cover-09.svg'
        WHEN 16000000000000010 THEN '/demo/contents/cover-10.svg'
        ELSE thumbnail_url END
WHERE content_id IN (16000000000000007,16000000000000008,
                     16000000000000009,16000000000000010)
  AND media_type='IMAGE'
  AND media_url LIKE '/demo/p1-media/%.png'
  AND thumbnail_url LIKE '/demo/p1-media/%.png';

UPDATE content_block
SET media_url = '/uploads/content/demo-article-1.jpg'
WHERE id=62502
  AND content_id=20003
  AND block_type='IMAGE'
  AND media_url='/demo/p1-media/cover-tactics.png';

UPDATE content
SET cover_url = CASE id
    WHEN 16000000000000007 THEN '/demo/contents/cover-07.svg'
    WHEN 16000000000000008 THEN '/demo/contents/cover-08.svg'
    WHEN 16000000000000009 THEN '/demo/contents/cover-09.svg'
    WHEN 16000000000000010 THEN '/demo/contents/cover-10.svg'
    ELSE cover_url END
WHERE id IN (16000000000000007,16000000000000008,
             16000000000000009,16000000000000010)
  AND cover_url LIKE '/demo/p1-media/%.png';

COMMIT;
