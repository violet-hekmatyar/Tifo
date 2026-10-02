-- VR8-M1 onboarding media repair.
-- Scope: the six live onboarding teams and six live onboarding players returned
-- by OnboardingService (ordered by follower_count DESC, id ASC).
-- No schema change, INSERT, DELETE or TRUNCATE. The procedure is idempotent and
-- refuses to overwrite a row whose identity or previous media value changed.

SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS vr8_m1_onboarding_media_seed;
DELIMITER $$
CREATE PROCEDURE vr8_m1_onboarding_media_seed()
BEGIN
    IF (SELECT COUNT(*) FROM football_team WHERE id IN (30001,30002,30003,30004,30005,30006)
        AND status='ACTIVE' AND is_deleted=0
        AND ((id=30001 AND team_name='Barcelona' AND logo_url IN ('/uploads/team/barcelona.png','/demo/p1-media/team-crest-crimson.png'))
          OR (id=30002 AND team_name='Real Madrid' AND logo_url IN ('/uploads/team/real-madrid.png','/demo/p1-media/team-crest-cobalt.png'))
          OR (id=30003 AND team_name='Bayern Munich' AND logo_url IN ('/uploads/team/bayern.png','/demo/p1-media/team-crest-crimson.png'))
          OR (id=30004 AND team_name='Manchester City' AND logo_url IN ('/uploads/team/man-city.png','/demo/p1-media/team-crest-cobalt.png'))
          OR (id=30005 AND team_name='Arsenal' AND logo_url IN ('/uploads/team/arsenal.png','/demo/p1-media/team-crest-crimson.png'))
          OR (id=30006 AND team_name='Liverpool' AND logo_url IN ('/uploads/team/liverpool.png','/demo/p1-media/team-crest-cobalt.png')))) <> 6 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR8-M1 team target conflict or missing row';
    END IF;

    IF (SELECT COUNT(*) FROM football_player WHERE id IN (40007,40001,40009,40002,40003,40004)
        AND status='ACTIVE' AND is_deleted=0
        AND ((id=40007 AND player_name='Erling Haaland' AND avatar_url IN ('/uploads/player/haaland.png','/demo/p1-media/player-flagship.png'))
          OR (id=40001 AND player_name='Robert Lewandowski' AND avatar_url IN ('/uploads/player/lewandowski.png','/demo/p1-media/player-flagship.png'))
          OR (id=40009 AND player_name='Mohamed Salah' AND avatar_url IN ('/uploads/player/salah.png','/demo/p1-media/player-flagship.png'))
          OR (id=40002 AND player_name='Pedri' AND avatar_url IN ('/uploads/player/pedri.png','/demo/p1-media/player-flagship.png'))
          OR (id=40003 AND player_name='Vinicius Junior' AND avatar_url IN ('/uploads/player/vinicius.png','/demo/p1-media/player-flagship.png'))
          OR (id=40004 AND player_name='Jude Bellingham' AND avatar_url IN ('/uploads/player/bellingham.png','/demo/p1-media/player-flagship.png')))) <> 6 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR8-M1 player target conflict or missing row';
    END IF;

    START TRANSACTION;
    UPDATE football_team SET logo_url='/demo/p1-media/team-crest-crimson.png'
      WHERE id=30001 AND logo_url IN ('/uploads/team/barcelona.png','/demo/p1-media/team-crest-crimson.png');
    UPDATE football_team SET logo_url='/demo/p1-media/team-crest-cobalt.png'
      WHERE id=30002 AND logo_url IN ('/uploads/team/real-madrid.png','/demo/p1-media/team-crest-cobalt.png');
    UPDATE football_team SET logo_url='/demo/p1-media/team-crest-crimson.png'
      WHERE id=30003 AND logo_url IN ('/uploads/team/bayern.png','/demo/p1-media/team-crest-crimson.png');
    UPDATE football_team SET logo_url='/demo/p1-media/team-crest-cobalt.png'
      WHERE id=30004 AND logo_url IN ('/uploads/team/man-city.png','/demo/p1-media/team-crest-cobalt.png');
    UPDATE football_team SET logo_url='/demo/p1-media/team-crest-crimson.png'
      WHERE id=30005 AND logo_url IN ('/uploads/team/arsenal.png','/demo/p1-media/team-crest-crimson.png');
    UPDATE football_team SET logo_url='/demo/p1-media/team-crest-cobalt.png'
      WHERE id=30006 AND logo_url IN ('/uploads/team/liverpool.png','/demo/p1-media/team-crest-cobalt.png');

    UPDATE football_player SET avatar_url='/demo/p1-media/player-flagship.png'
      WHERE id IN (40007,40001,40009,40002,40003,40004)
        AND avatar_url IN ('/uploads/player/haaland.png','/uploads/player/lewandowski.png','/uploads/player/salah.png','/uploads/player/pedri.png','/uploads/player/vinicius.png','/uploads/player/bellingham.png','/demo/p1-media/player-flagship.png');
    COMMIT;
END$$
DELIMITER ;
CALL vr8_m1_onboarding_media_seed();
DROP PROCEDURE IF EXISTS vr8_m1_onboarding_media_seed;

SELECT 'vr8_m1_seed' AS metric, 6 AS teams, 6 AS players;
