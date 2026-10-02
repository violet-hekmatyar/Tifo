-- VR8-M1 protected rollback. It only restores exact original values when the
-- current value is the VR8 value; it never removes rows or P1 media.
SET NAMES utf8mb4;
START TRANSACTION;
UPDATE football_team SET logo_url=CASE id
  WHEN 30001 THEN '/uploads/team/barcelona.png'
  WHEN 30002 THEN '/uploads/team/real-madrid.png'
  WHEN 30003 THEN '/uploads/team/bayern.png'
  WHEN 30004 THEN '/uploads/team/man-city.png'
  WHEN 30005 THEN '/uploads/team/arsenal.png'
  WHEN 30006 THEN '/uploads/team/liverpool.png'
END
WHERE id IN (30001,30002,30003,30004,30005,30006)
  AND (
    (id IN (30001,30003,30005)
      AND logo_url='/demo/p1-media/team-crest-crimson.png')
    OR (id IN (30002,30004,30006)
      AND logo_url='/demo/p1-media/team-crest-cobalt.png')
  );
UPDATE football_player SET avatar_url=CASE id
  WHEN 40007 THEN '/uploads/player/haaland.png'
  WHEN 40001 THEN '/uploads/player/lewandowski.png'
  WHEN 40009 THEN '/uploads/player/salah.png'
  WHEN 40002 THEN '/uploads/player/pedri.png'
  WHEN 40003 THEN '/uploads/player/vinicius.png'
  WHEN 40004 THEN '/uploads/player/bellingham.png'
END
WHERE id IN (40007,40001,40009,40002,40003,40004)
  AND avatar_url='/demo/p1-media/player-flagship.png';
COMMIT;
