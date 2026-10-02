-- VR14-M1 additive DEMO media repair.
-- Scope is limited to fixed DEMO teams, players, user profiles and legacy
-- content rows used by the four root pages. No INSERT/DELETE/TRUNCATE.
-- Notification rows are intentionally not rewritten: VR11 already owns the
-- eight-row baseline and the validator below checks it read-only.

USE south_stand;
SET NAMES utf8mb4;
START TRANSACTION;

-- Root data teams used by the data page and the anonymous/demo feed.
UPDATE football_team
SET logo_url = CASE id
    WHEN 30001 THEN '/demo/p1-media/team-barcelona.png'
    WHEN 30002 THEN '/demo/p1-media/team-real-madrid.png'
    WHEN 30003 THEN '/demo/p1-media/team-bayern.png'
    WHEN 30004 THEN '/demo/p1-media/team-man-city.png'
    WHEN 30005 THEN '/demo/p1-media/team-arsenal.png'
    WHEN 30006 THEN '/demo/p1-media/team-liverpool.png'
    WHEN 13000000000000010 THEN '/demo/p1-media/team-inter.png'
    WHEN 13000000000000011 THEN '/demo/p1-media/team-crest-crimson.png'
    WHEN 13000000000000012 THEN '/demo/p1-media/team-crest-cobalt.png'
    ELSE logo_url END
WHERE id IN (30001,30002,30003,30004,30005,30006,
             13000000000000010,13000000000000011,13000000000000012)
  AND status = 'ACTIVE' AND is_deleted = 0
  AND (
      (id=30001 AND team_name='Barcelona') OR
      (id=30002 AND team_name='Real Madrid') OR
      (id=30003 AND team_name='Bayern Munich') OR
      (id=30004 AND team_name='Manchester City') OR
      (id=30005 AND team_name='Arsenal') OR
      (id=30006 AND team_name='Liverpool') OR
      (id=13000000000000010 AND team_name='国际米兰') OR
      (id=13000000000000011 AND team_name='AC米兰') OR
      (id=13000000000000012 AND team_name='尤文图斯')
  );

-- The remaining fixed DEMO football rows used by the data-page league/team
-- selectors previously pointed to placeholder SVG paths. Those paths resolve
-- to JSON in the local backend, so map each row to its own verified PNG.
UPDATE football_team
SET logo_url = CASE id
    WHEN 13000000000000001 THEN '/demo/p1-media/team-demo-01.png'
    WHEN 13000000000000002 THEN '/demo/p1-media/team-demo-02.png'
    WHEN 13000000000000003 THEN '/demo/p1-media/team-demo-03.png'
    WHEN 13000000000000004 THEN '/demo/p1-media/team-demo-04.png'
    WHEN 13000000000000005 THEN '/demo/p1-media/team-demo-05.png'
    WHEN 13000000000000006 THEN '/demo/p1-media/team-demo-06.png'
    WHEN 13000000000000007 THEN '/demo/p1-media/team-demo-07.png'
    WHEN 13000000000000008 THEN '/demo/p1-media/team-demo-08.png'
    WHEN 13000000000000009 THEN '/demo/p1-media/team-demo-09.png'
    WHEN 13000000000000013 THEN '/demo/p1-media/team-demo-10.png'
    WHEN 13000000000000014 THEN '/demo/p1-media/team-demo-11.png'
    WHEN 13000000000000015 THEN '/demo/p1-media/team-demo-12.png'
    WHEN 13000000000000016 THEN '/demo/p1-media/team-demo-13.png'
    WHEN 13000000000000017 THEN '/demo/p1-media/team-demo-14.png'
    WHEN 13000000000000018 THEN '/demo/p1-media/team-demo-15.png'
    WHEN 13000000000000019 THEN '/demo/p1-media/team-demo-16.png'
    WHEN 13000000000000020 THEN '/demo/p1-media/team-demo-17.png'
    WHEN 13000000000000021 THEN '/demo/p1-media/team-demo-18.png'
    WHEN 13000000000000022 THEN '/demo/p1-media/team-demo-19.png'
    WHEN 13000000000000023 THEN '/demo/p1-media/team-demo-20.png'
    WHEN 13000000000000024 THEN '/demo/p1-media/team-demo-21.png'
    ELSE logo_url END
WHERE id IN (13000000000000001,13000000000000002,13000000000000003,
             13000000000000004,13000000000000005,13000000000000006,
             13000000000000007,13000000000000008,13000000000000009,
             13000000000000013,13000000000000014,13000000000000015,
             13000000000000016,13000000000000017,13000000000000018,
             13000000000000019,13000000000000020,13000000000000021,
             13000000000000022,13000000000000023,13000000000000024)
  AND status='ACTIVE' AND is_deleted=0
  AND logo_url LIKE '/demo/teams/%.svg';

UPDATE football_league
SET logo_url = CASE id
    WHEN 12000000000000001 THEN '/demo/p1-media/team-demo-01.png'
    WHEN 12000000000000002 THEN '/demo/p1-media/team-demo-02.png'
    WHEN 12000000000000003 THEN '/demo/p1-media/team-demo-03.png'
    WHEN 12000000000000004 THEN '/demo/p1-media/team-demo-04.png'
    WHEN 12000000000000005 THEN '/demo/p1-media/team-demo-05.png'
    WHEN 12000000000000006 THEN '/demo/p1-media/team-demo-06.png'
    WHEN 12000000000000007 THEN '/demo/p1-media/team-demo-07.png'
    WHEN 12000000000000008 THEN '/demo/p1-media/team-demo-08.png'
    ELSE logo_url END
WHERE id IN (12000000000000001,12000000000000002,12000000000000003,
             12000000000000004,12000000000000005,12000000000000006,
             12000000000000007,12000000000000008)
  AND status='ACTIVE' AND is_deleted=0
  AND logo_url LIKE '/demo/teams/%.svg';

-- Root data and profile player avatars. Reuse is intentional for player
-- portraits; team identities above never share a normal-state team crest.
UPDATE football_player
SET avatar_url = '/demo/p1-media/player-flagship.png'
WHERE (
    id BETWEEN 40001 AND 40010 OR
    id IN (14000000000000001,14000000000000004,14000000000000010,
           14000000000000019,14000000000000028,14000000000000061,
           14000000000000067)
  )
  AND status = 'ACTIVE' AND is_deleted = 0;

-- The existing test_user and VR11 actors are DEMO identities used by the
-- profile and notification roots. Keep the update constrained to fixed IDs.
UPDATE user_profile
SET avatar_url = '/demo/p1-media/user-demo.png'
WHERE user_id IN (
    10002,
    11000000000000001,11000000000000002,11000000000000003,
    11000000000000004,11000000000000005,11000000000000006,
    11000000000000007,11000000000000008,11000000000000009,
    11000000000000010,11000000000000015,11000000000000017,
    11000000000000022,11000000000000029
  )
  AND status = 'ACTIVE' AND is_deleted = 0;

-- Legacy root/profile content rows had missing /uploads assets. Map only the
-- fixed local-demo records to verified PNGs already served by the backend.
UPDATE content
SET cover_url = CASE id
    WHEN 20001 THEN '/demo/p1-media/cover-stadium.png'
    WHEN 20002 THEN '/demo/p1-media/cover-stadium.png'
    WHEN 20003 THEN '/demo/p1-media/cover-tactics.png'
    WHEN 20004 THEN '/demo/p1-media/cover-stadium.png'
    WHEN 20005 THEN '/demo/p1-media/cover-training.png'
    WHEN 20006 THEN '/demo/p1-media/cover-training.png'
    ELSE cover_url END
WHERE id BETWEEN 20001 AND 20006
  AND author_id IN (10001,10002)
  AND status = 'PUBLISHED' AND is_deleted = 0;

UPDATE content
SET cover_url = CASE id
    WHEN 16000000000000007 THEN '/demo/p1-media/cover-stadium.png'
    WHEN 16000000000000008 THEN '/demo/p1-media/cover-training.png'
    WHEN 16000000000000009 THEN '/demo/p1-media/cover-tactics.png'
    WHEN 16000000000000010 THEN '/demo/p1-media/cover-stadium.png'
    ELSE cover_url END
WHERE id IN (16000000000000007,16000000000000008,
             16000000000000009,16000000000000010)
  AND status='PUBLISHED' AND is_deleted=0
  AND cover_url LIKE '/demo/contents/%.svg';

UPDATE content_media
SET media_url = CASE content_id
        WHEN 20001 THEN '/demo/p1-media/cover-stadium.png'
        WHEN 20003 THEN '/demo/p1-media/cover-tactics.png'
        WHEN 20004 THEN '/demo/p1-media/cover-stadium.png'
        WHEN 20005 THEN '/demo/p1-media/cover-training.png'
        ELSE media_url END,
    thumbnail_url = CASE content_id
        WHEN 20001 THEN '/demo/p1-media/cover-stadium.png'
        WHEN 20003 THEN '/demo/p1-media/cover-tactics.png'
        WHEN 20004 THEN '/demo/p1-media/cover-stadium.png'
        WHEN 20005 THEN '/demo/p1-media/cover-training.png'
        ELSE thumbnail_url END
WHERE content_id IN (20001,20003,20004,20005)
  AND media_type = 'IMAGE' AND status = 'ACTIVE' AND is_deleted = 0;

-- The four root/profile content rows also have legacy media rows. Keep the
-- cover and media fields on the same verified PNG asset.
UPDATE content_media
SET media_url = CASE content_id
        WHEN 16000000000000007 THEN '/demo/p1-media/cover-stadium.png'
        WHEN 16000000000000008 THEN '/demo/p1-media/cover-training.png'
        WHEN 16000000000000009 THEN '/demo/p1-media/cover-tactics.png'
        WHEN 16000000000000010 THEN '/demo/p1-media/cover-stadium.png'
        ELSE media_url END,
    thumbnail_url = CASE content_id
        WHEN 16000000000000007 THEN '/demo/p1-media/cover-stadium.png'
        WHEN 16000000000000008 THEN '/demo/p1-media/cover-training.png'
        WHEN 16000000000000009 THEN '/demo/p1-media/cover-tactics.png'
        WHEN 16000000000000010 THEN '/demo/p1-media/cover-stadium.png'
        ELSE thumbnail_url END
WHERE content_id IN (16000000000000007,16000000000000008,
                     16000000000000009,16000000000000010)
  AND media_type = 'IMAGE' AND status = 'ACTIVE' AND is_deleted = 0;

UPDATE content_block
SET media_url = '/demo/p1-media/cover-tactics.png'
WHERE content_id = 20003
  AND block_type = 'IMAGE'
  AND status = 'ACTIVE' AND is_deleted = 0;

COMMIT;

SELECT 'vr14_m1_media_seed_targets' metric,
       'teams=30 players=17 user_profiles=11 contents=10 content_media=8 content_blocks=1' value;
