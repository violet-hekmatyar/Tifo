-- P1-M1 media-only incremental seed.
-- Scope: existing DEMO teams 11/12, players 61/67, users 1-3, contents 1-6.
-- Idempotent: updates fixed natural-key/ID targets only; no INSERT/DELETE/TRUNCATE.
-- Run in south_stand after the matching backend static resources are available.

SET NAMES utf8mb4;
START TRANSACTION;

-- The selected IDs are DEMO ranges. The name predicates are a conflict guard: if
-- a target has been repurposed, that row is left untouched and the validator fails.
UPDATE football_team
SET logo_url = CASE id
    WHEN 13000000000000011 THEN '/demo/p1-media/team-crest-crimson.png'
    WHEN 13000000000000012 THEN '/demo/p1-media/team-crest-cobalt.png'
    ELSE logo_url END
WHERE id IN (13000000000000011, 13000000000000012)
  AND team_name IN ('AC米兰', '尤文图斯')
  AND status = 'ACTIVE' AND is_deleted = 0;

UPDATE football_player
SET avatar_url = '/demo/p1-media/player-flagship.png'
WHERE id IN (14000000000000061, 14000000000000067)
  AND status = 'ACTIVE' AND is_deleted = 0;

UPDATE user_profile p
JOIN sys_user u ON u.id = p.user_id
SET p.avatar_url = '/demo/p1-media/user-demo.png'
WHERE p.user_id IN (11000000000000001, 11000000000000002, 11000000000000003)
  AND u.username IN ('demo_user_01', 'demo_user_02', 'demo_user_03')
  AND u.status = 'ACTIVE'
  AND p.status = 'ACTIVE' AND p.is_deleted = 0;

UPDATE content
SET cover_url = CASE id
    WHEN 16000000000000001 THEN '/demo/p1-media/cover-stadium.png'
    WHEN 16000000000000002 THEN '/demo/p1-media/cover-training.png'
    WHEN 16000000000000003 THEN '/demo/p1-media/cover-tactics.png'
    WHEN 16000000000000004 THEN '/demo/p1-media/cover-stadium.png'
    WHEN 16000000000000005 THEN '/demo/p1-media/cover-training.png'
    WHEN 16000000000000006 THEN '/demo/p1-media/cover-tactics.png'
    ELSE cover_url END
WHERE id BETWEEN 16000000000000001 AND 16000000000000006
  AND author_id BETWEEN 11000000000000001 AND 11000000000000036
  AND status = 'PUBLISHED' AND is_deleted = 0;

UPDATE content_media
SET media_url = CASE content_id
    WHEN 16000000000000001 THEN '/demo/p1-media/cover-stadium.png'
    WHEN 16000000000000002 THEN '/demo/p1-media/cover-training.png'
    WHEN 16000000000000003 THEN '/demo/p1-media/cover-tactics.png'
    WHEN 16000000000000004 THEN '/demo/p1-media/cover-stadium.png'
    WHEN 16000000000000005 THEN '/demo/p1-media/cover-training.png'
    WHEN 16000000000000006 THEN '/demo/p1-media/cover-tactics.png'
    ELSE media_url END
WHERE content_id BETWEEN 16000000000000001 AND 16000000000000006
  AND media_type = 'IMAGE' AND status = 'ACTIVE' AND is_deleted = 0;

COMMIT;

-- The result is intentionally queryable by the validator and by the M1 smoke.
SELECT 'p1_m1_media_seed_targets' metric, 2 teams, 2 players, 3 users, 6 contents;
