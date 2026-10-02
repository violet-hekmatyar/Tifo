-- P1-M1 targeted rollback. Only values written by P1_M1_MEDIA_SEED.sql match.
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE football_team
SET logo_url = CASE id
    WHEN 13000000000000011 THEN '/demo/teams/team-11.svg'
    WHEN 13000000000000012 THEN '/demo/teams/team-12.svg'
    ELSE logo_url END
WHERE id IN (13000000000000011, 13000000000000012)
  AND logo_url LIKE '/demo/p1-media/%';

UPDATE football_player
SET avatar_url = '/demo/players/player-01.svg'
WHERE id = 14000000000000061 AND avatar_url = '/demo/p1-media/player-flagship.png';
UPDATE football_player
SET avatar_url = '/demo/players/player-07.svg'
WHERE id = 14000000000000067 AND avatar_url = '/demo/p1-media/player-flagship.png';

UPDATE user_profile
SET avatar_url = '/demo/users/user-01.svg'
WHERE user_id IN (11000000000000001, 11000000000000002, 11000000000000003)
  AND avatar_url = '/demo/p1-media/user-demo.png';

UPDATE content
SET cover_url = CASE id
    WHEN 16000000000000001 THEN '/demo/contents/cover-01.svg'
    WHEN 16000000000000002 THEN '/demo/contents/cover-02.svg'
    WHEN 16000000000000003 THEN '/demo/contents/cover-03.svg'
    WHEN 16000000000000004 THEN '/demo/contents/cover-04.svg'
    WHEN 16000000000000005 THEN '/demo/contents/cover-05.svg'
    WHEN 16000000000000006 THEN '/demo/contents/cover-06.svg'
    ELSE cover_url END
WHERE id BETWEEN 16000000000000001 AND 16000000000000006
  AND cover_url LIKE '/demo/p1-media/%';

UPDATE content_media
SET media_url = CASE content_id
    WHEN 16000000000000001 THEN '/demo/contents/cover-01.svg'
    WHEN 16000000000000002 THEN '/demo/contents/cover-02.svg'
    WHEN 16000000000000003 THEN '/demo/contents/cover-03.svg'
    WHEN 16000000000000004 THEN '/demo/contents/cover-04.svg'
    WHEN 16000000000000005 THEN '/demo/contents/cover-05.svg'
    WHEN 16000000000000006 THEN '/demo/contents/cover-06.svg'
    ELSE media_url END
WHERE content_id BETWEEN 16000000000000001 AND 16000000000000006
  AND media_url LIKE '/demo/p1-media/%';

COMMIT;
