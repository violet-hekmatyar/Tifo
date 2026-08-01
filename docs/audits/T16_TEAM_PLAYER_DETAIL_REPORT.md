# T16 球队阵容、球队详情增强与球员生涯完成报告

## 1. 实现范围

新增 `football/detail` 下 3 个实体、3 个 Mapper、聚合 VO 和 `FootballDetailService`；扩展球队/球员 Controller、schema、增量生成器、SQL、测试、Windows 脚本及文档。

## 2. 表与数据来源

新增 `football_team_season_player`、`football_team_honor`、`football_player_team_history`。保留 `team_player` 作为当前球队主关系；球队和球员赛季统计继续复用 T15 三张统计表，没有重复统计表。

阵容由有效 `team_player` 与 T15 player stat 交集生成。历史只为 154 名球员各补一条可验证当前效力记录，没有伪造转会。球队 overview 聚合基本资料、联赛赛季、积分榜、球队统计、阵容、比赛和关联内容；career 按赛季和球队聚合 T15 记录，评分按出场数加权。

## 3. API

新增球队 overview、players、stats、honors，以及球员 overview、stats、teams、career 共 8 个公开只读接口。

## 4. Demo、保护和冲突

实际补充 358 条赛季阵容、60 条明确标记 Demo 的荣誉、154 条当前效力记录。16 项 T16 SQL 一致性检查全部为 0。首次导入前 4022 条旧记录逐 ID 与关键字段 SHA-256 比对通过；最终连续双跑前的 4178 条记录也全部保持不变。没有覆盖非 DEMO 数据，没有强行插入错误关系。

## 5. 幂等、性能和测试

增量 seed 使用业务唯一键 `NOT EXISTS`；连续执行两次均保持 358 阵容、60 荣誉、154 历史。阵容、球队、球员、关注状态、比赛和内容均批量查询，循环中没有 Mapper 调用，不存在随记录数增长的 N+1。当前聚合优先保证常数级批量查询，overview 仍有可合并的重复作用域读取，后续可继续压缩 SQL 常数。

本机 smoke 单次耗时：球队 overview 40ms、stats 8ms、players 11ms、honors 6ms；球员 overview 34ms、stats 8ms、teams 7ms、career 7ms。`mvn test` 在数据库环境下 84 项全部通过、0 跳过；`check-t16.ps1` 的 T03-T16 smoke、前后数据指纹和 8080/8090 清理全部通过。

## 6. 遗留问题

T16 是展示用 Demo 数据，不代表官方荣誉或实时转会。可靠历史不足，因此不扩写不可信历史链；`preferredFoot` 无事实源时返回 null。比赛首发和逐场球员技术统计留待 T17。

## 7. 最终结论

```text
旧数据是否保留：是
T16 新数据是否补充成功：是
球队阵容是否可用：是
球队详情是否可用：是
球员详情是否可用：是
球员生涯是否可用：是
是否存在未解决冲突：否
是否存在 N+1：否
是否可以提交：是
```

本轮未执行 Git 提交、推送或暂存。

## 8. 验收清单

1. 修改和新增文件：详情领域代码、三张表、迁移/seed/validate、测试、smoke/check 和文档。
2. 新增或复用表：新增三张 T16 表，复用 `team_player` 与 T15 统计表。
3. 新增接口：8 个公开只读接口。
4. 阵容模型：赛季、球队、球员唯一，支持位置、号码、角色、队长和租借。
5. 是否复用 team_player：是，作为当前主关系校验源。
6. 球队 overview：常数级聚合球队、赛季、榜单、统计、阵容、比赛和内容。
7. 球队 stats：直接复用 T15 team stat 与 standing。
8. 球员 stats：直接复用 T15 player stat。
9. 球员历史：只保存可证明当前关系，不伪造转会。
10. career：按 T15 记录聚合总计、赛季和球队，评分按出场加权。
11. T16 Demo 数量：358、60、154。
12. 旧用户/内容/互动是否保留：是，指纹验证通过。
13. 是否遇到冲突：未发现数据冲突；开发中修复了空批量 ID 查询。
14. 是否存在强行插入：否。
15. 增量是否幂等：是，连续两次数量一致。
16. validate 结果：16 项 SQL 与离线结构检查全部通过。
17. 是否存在 N+1：否。
18. SQL 次数和耗时：查询数为常数；实测耗时见上一节，overview 重复作用域查询仍可优化。
19. mvn test：84 项通过，数据库环境 0 跳过。
20. check-t16：T03-T16 smoke 和数据保护全部通过。
21. 遗留问题：官方数据、可靠转会历史、惯用脚和 T17 比赛级数据尚未接入。
22. git status：工作区仅保留本轮修改，未 add、commit 或 push；以最终 `git status --short` 为准。
