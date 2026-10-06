USE south_stand;
SET NAMES utf8mb4;

-- 一次性脚本：退役 seed-demo.sql 写入的演示数据，让 App 只显示 football-data.org 的真实数据。
--
-- 退役方式：软删（is_deleted=1，有 status 列的表同时置 DISABLED）。
--   软删而非物理删的原因：follow_record / comment / content_relation 等引用不悬空，
--   后端查询一律带 is_deleted=0（FootballQueryService.activeLeagueWrapper 等），App 自然隐藏；
--   需要时用 retire-demo-data-rollback.sql 整体回滚。
--
-- 范围 = T14 演示数据集按业务域分配的 ID 段（每段宽 1e15，取自 scripts/data/README.md）：
--   11 = 演示账号     12 = 联赛      13 = 球队      14 = 球员/阵容
--   15 = 比赛/事件    20 = 赛季      21 = 阶段      22 = 积分榜
--   23 = 球员榜       24 = 球队榜
-- 真实数据是 football-data.org 的全局 ID（量级 1e3~1e7：英超 2021、阿森纳 57），
-- 与这些 1.1e16~2.5e16 的段完全不相交。
--
-- 不在本脚本范围内（有意保留）：
--   - seed.sql 的本地 dev smoke 账号（sys_user 10001-10004 / user_profile 11001-11004）：
--     check-*/smoke-* 脚本依赖它们登录（密码 password），退役会打断开发工具链。
--     它们对应的 main_team_id 指向被退役的 30001 球队，个人页会显示空，属已知无害残留。
--   - 演示内容域（16 = content / 17 = comment / 18 = interaction）：退役账号后其帖子仍在信息流里。
--     如需一并清理，按同样的 ID 段处理即可，但注意 content_media / content_block / content_relation
--     用 is_deleted，而 like_record / favorite_record 没有 is_deleted 列（只有 status，需置 CANCELLED）。
--   - P1-M2 / VR11 / VR14 等测试夹具自己的 ID 段（3.6e16~3.8e16）：它们的联赛/球队/比赛父行在上面这段里，
--     退役后 App 自然看不到；保留子行是为了不弄坏 check-t15~t22 等冒烟脚本。
--
-- 执行前建议先备份：
--   docker exec apihub-mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --default-character-set=utf8mb4 south_stand' > backup_before_retire.sql

START TRANSACTION;

-- 用户域：演示账号
UPDATE sys_user SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 11000000000000000 AND id < 12000000000000000 AND is_deleted = 0;
UPDATE user_profile SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 11000000000000000 AND id < 12000000000000000 AND is_deleted = 0;
UPDATE user_onboarding SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 11000000000000000 AND id < 12000000000000000 AND is_deleted = 0;

-- 足球域：赛事与赛季主数据
UPDATE football_league SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 12000000000000000 AND id < 13000000000000000 AND is_deleted = 0;
UPDATE football_season SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 20000000000000000 AND id < 21000000000000000 AND is_deleted = 0;
UPDATE football_competition_stage SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 21000000000000000 AND id < 22000000000000000 AND is_deleted = 0;

-- 足球域：球队 / 球员 / 阵容
UPDATE football_team SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 13000000000000000 AND id < 14000000000000000 AND is_deleted = 0;
UPDATE football_player SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 14000000000000000 AND id < 15000000000000000 AND is_deleted = 0;
UPDATE team_player SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 14000000000000000 AND id < 15000000000000000 AND is_deleted = 0;

-- 足球域：比赛与事件
UPDATE match_info SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 15000000000000000 AND id < 16000000000000000 AND is_deleted = 0;
UPDATE match_event SET status = 'DISABLED', is_deleted = 1
 WHERE id >= 15000000000000000 AND id < 16000000000000000 AND is_deleted = 0;

-- 足球域：榜单（这几张表没有 status 列）
UPDATE football_standing SET is_deleted = 1
 WHERE id >= 22000000000000000 AND id < 23000000000000000 AND is_deleted = 0;
UPDATE football_player_competition_stat SET is_deleted = 1
 WHERE id >= 23000000000000000 AND id < 24000000000000000 AND is_deleted = 0;
UPDATE football_team_competition_stat SET is_deleted = 1
 WHERE id >= 24000000000000000 AND id < 25000000000000000 AND is_deleted = 0;

-- seed.sql 的本地 dev smoke 数据集用的是小 ID，与上面的段不重叠，也与真实数据不重叠
-- （已核对：真实 league 2000~2021、team 1~11034、match 536968+），因此按显式 ID 列表退役。
-- 10001-10003 联赛不退役的话，App 会同时显示 "Premier League"(10001) 和真实的 "英超"(2021)。
UPDATE football_league SET status = 'DISABLED', is_deleted = 1
 WHERE id IN (10001, 10002, 10003) AND is_deleted = 0;
UPDATE football_team SET status = 'DISABLED', is_deleted = 1
 WHERE id IN (30001, 30002, 30003, 30004, 30005, 30006) AND is_deleted = 0;
UPDATE football_player SET status = 'DISABLED', is_deleted = 1
 WHERE id IN (40001, 40002, 40003, 40004, 40005, 40006, 40007, 40008, 40009, 40010) AND is_deleted = 0;
UPDATE team_player SET status = 'DISABLED', is_deleted = 1
 WHERE id IN (41001, 41002, 41003, 41004, 41005, 41006, 41007, 41008, 41009, 41010) AND is_deleted = 0;
UPDATE match_info SET status = 'DISABLED', is_deleted = 1
 WHERE id IN (50001, 50002, 50003, 50004, 50005, 50006) AND is_deleted = 0;
UPDATE match_event SET status = 'DISABLED', is_deleted = 1
 WHERE id IN (51001, 51002, 51003, 51004, 51005, 51006, 51007) AND is_deleted = 0;

COMMIT;

-- 可选：内容域（演示帖子/评论/互动）默认不执行，原因见文件头说明。
-- 注意 like_record / favorite_record 没有 is_deleted 列，只有 status，不能照搬上面的写法。

-- 校验见 scripts/sql/validate-retired-demo-data.sql
