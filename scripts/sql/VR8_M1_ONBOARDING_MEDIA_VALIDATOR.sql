-- VR8-M1 validator. Every result should be zero except the target count rows.
SET NAMES utf8mb4;
SELECT 'target_team_count' AS check_name, COUNT(*) AS value
FROM football_team WHERE id IN (30001,30002,30003,30004,30005,30006)
  AND logo_url LIKE '/demo/p1-media/%.png';
SELECT 'target_player_count' AS check_name, COUNT(*) AS value
FROM football_player WHERE id IN (40007,40001,40009,40002,40003,40004)
  AND avatar_url='/demo/p1-media/player-flagship.png';
SELECT 'team_target_rows_not_in_vr8_targets' AS check_name, COUNT(*) AS value
FROM football_team
WHERE id IN (30001,30002,30003,30004,30005,30006)
  AND logo_url NOT IN ('/demo/p1-media/team-crest-crimson.png','/demo/p1-media/team-crest-cobalt.png');
SELECT 'player_target_rows_not_in_vr8_target' AS check_name, COUNT(*) AS value
FROM football_player
WHERE id IN (40007,40001,40009,40002,40003,40004)
  AND avatar_url<>'/demo/p1-media/player-flagship.png';
SELECT id, team_name, logo_url FROM football_team
WHERE id IN (30001,30002,30003,30004,30005,30006) ORDER BY follower_count DESC,id;
SELECT id, player_name, avatar_url FROM football_player
WHERE id IN (40007,40001,40009,40002,40003,40004) ORDER BY follower_count DESC,id;
