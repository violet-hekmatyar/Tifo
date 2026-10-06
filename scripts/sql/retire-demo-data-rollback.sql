USE south_stand;
SET NAMES utf8mb4;

-- retire-demo-data.sql 的回滚：把演示数据段整体恢复为可用状态。
-- 注意：会把该 ID 段内因其他原因软删过的行一并复活（本项目里这些段只属于演示数据）。

START TRANSACTION;

UPDATE sys_user SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 11000000000000000 AND id < 12000000000000000;
UPDATE user_profile SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 11000000000000000 AND id < 12000000000000000;
UPDATE user_onboarding SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 11000000000000000 AND id < 12000000000000000;

UPDATE football_league SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 12000000000000000 AND id < 13000000000000000;
UPDATE football_season SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 20000000000000000 AND id < 21000000000000000;
UPDATE football_competition_stage SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 21000000000000000 AND id < 22000000000000000;

UPDATE football_team SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 13000000000000000 AND id < 14000000000000000;
UPDATE football_player SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 14000000000000000 AND id < 15000000000000000;
UPDATE team_player SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 14000000000000000 AND id < 15000000000000000;

UPDATE match_info SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 15000000000000000 AND id < 16000000000000000;
UPDATE match_event SET status = 'ACTIVE', is_deleted = 0
 WHERE id >= 15000000000000000 AND id < 16000000000000000;

UPDATE football_standing SET is_deleted = 0
 WHERE id >= 22000000000000000 AND id < 23000000000000000;
UPDATE football_player_competition_stat SET is_deleted = 0
 WHERE id >= 23000000000000000 AND id < 24000000000000000;
UPDATE football_team_competition_stat SET is_deleted = 0
 WHERE id >= 24000000000000000 AND id < 25000000000000000;

-- seed.sql 的本地 dev smoke 足球行（显式 ID 列表）
UPDATE football_league SET status = 'ACTIVE', is_deleted = 0
 WHERE id IN (10001, 10002, 10003);
UPDATE football_team SET status = 'ACTIVE', is_deleted = 0
 WHERE id IN (30001, 30002, 30003, 30004, 30005, 30006);
UPDATE football_player SET status = 'ACTIVE', is_deleted = 0
 WHERE id IN (40001, 40002, 40003, 40004, 40005, 40006, 40007, 40008, 40009, 40010);
UPDATE team_player SET status = 'ACTIVE', is_deleted = 0
 WHERE id IN (41001, 41002, 41003, 41004, 41005, 41006, 41007, 41008, 41009, 41010);
UPDATE match_info SET status = 'ACTIVE', is_deleted = 0
 WHERE id IN (50001, 50002, 50003, 50004, 50005, 50006);
UPDATE match_event SET status = 'ACTIVE', is_deleted = 0
 WHERE id IN (51001, 51002, 51003, 51004, 51005, 51006, 51007);

-- 注意：内容域（content/comment/互动）不在 retire-demo-data.sql 范围内，这里不做恢复。

COMMIT;
