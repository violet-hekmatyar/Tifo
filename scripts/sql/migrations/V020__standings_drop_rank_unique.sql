USE south_stand;
SET NAMES utf8mb4;

-- V020：删除 football_standing 上的 uk_standing_scope_rank 唯一键。
--
-- 原因：该键要求 (league_id, season_id, stage_id, group_code) 内 rank_no 唯一，
-- 意味着源数据出现一次重复名次就会让整批数据导入失败——把上游数据质量问题
-- 变成了本地硬失败，属于要消除的互相制约。后端只按 scope 等值查询 + rank_no 排序，
-- 不依赖该唯一键；保留 uk_standing_scope_team 作为幂等锚点。
--
-- 幂等：索引不存在时直接跳过，可重复执行。schema.sql 与 V015 已同步移除此键，
-- 因此重跑 init-demo-data.ps1 不会把它加回来。

DROP PROCEDURE IF EXISTS v020_drop_rank_unique;
DELIMITER $$
CREATE PROCEDURE v020_drop_rank_unique()
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.statistics
             WHERE table_schema = DATABASE() AND table_name = 'football_standing'
               AND index_name = 'uk_standing_scope_rank') THEN
    ALTER TABLE football_standing DROP INDEX uk_standing_scope_rank;
  END IF;
END$$
DELIMITER ;
CALL v020_drop_rank_unique();
DROP PROCEDURE v020_drop_rank_unique;

SELECT IF(COUNT(*) = 0, 'V020 OK: uk_standing_scope_rank removed', 'V020 FAILED: index still present') AS result
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'football_standing' AND index_name = 'uk_standing_scope_rank';
