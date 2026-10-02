-- VR14-M2 DEMO feed composition seed.
-- Tunes hot_score on fixed VR14/P1 demonstration content so the
-- existing feed service can surface its real MATCH/RANKING/PLAYER_RATING and
-- DISCUSSION cards before the generic content cards. No API or algorithm
-- changes are involved.

USE south_stand;
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE content
SET hot_score = MOD(id - 16000000000000001, 15)
WHERE id BETWEEN 16000000000000001 AND 16000000000000120
  AND author_id BETWEEN 11000000000000001 AND 11000000000000035
  AND source_type='USER' AND status='PUBLISHED' AND is_deleted=0;

UPDATE content
SET hot_score = 130.00 - (id - 16000000000000201)
WHERE id BETWEEN 16000000000000201 AND 16000000000000210
  AND author_id BETWEEN 11000000000000001 AND 11000000000000010
  AND source_type='USER' AND remark='P1_M3_DEMO_CONTENT'
  AND status='PUBLISHED' AND is_deleted=0;

-- VR9 DEMO profile posts also participate in the public recommendation feed;
-- lower only these fixed records so they do not crowd the root-page first
-- viewport while remaining available in the profile content tab.
UPDATE content
SET hot_score = 14.00 - (id - 16000000000000901)
WHERE id BETWEEN 16000000000000901 AND 16000000000000912
  AND author_id IN (10002,11000000000000016)
  AND source_type='USER' AND remark='VR9_R1_DEMO_CONTENT'
  AND status='PUBLISHED' AND is_deleted=0;

UPDATE content
SET hot_score = CASE id
    WHEN 20001 THEN 5.00
    WHEN 20002 THEN 4.00
    WHEN 20003 THEN 3.00
    WHEN 20004 THEN 2.00
    WHEN 20005 THEN 1.00
    WHEN 20006 THEN 0.00
    ELSE hot_score END
WHERE id BETWEEN 20001 AND 20006
  AND author_id IN (10001,10002)
  AND source_type IN ('ADMIN','USER')
  AND status='PUBLISHED' AND is_deleted=0;

COMMIT;

SELECT 'vr14_m2_home_feed_seed' metric, 'fixed_demo_content_hot_scores_tuned' value;
