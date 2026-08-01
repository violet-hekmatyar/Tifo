# T15 赛季、积分榜、球员榜与球队榜完成报告

## 1. 修改和新增文件

新增 `football/rank` 下的 5 个 Entity、5 个 Mapper、2 个榜单枚举、6 个 VO、`FootballRankService` 和 `FootballRankController`；扩展 `LeagueController`、`SecurityConfig`、schema、Demo 生成器、校验程序、测试、Windows 脚本及项目文档。

## 2. 新增表和索引

新增 `football_season`、`football_competition_stage`、`football_standing`、`football_player_competition_stat`、`football_team_competition_stat`。表包含赛事作用域唯一键和按联赛、赛季、阶段查询的组合索引，榜单实体索引不依赖用户输入。

## 3. 新增接口

```text
GET /api/app/football/leagues/{leagueId}/seasons
GET /api/app/football/leagues/{leagueId}/seasons/{seasonId}/stages
GET /api/app/football/standings
GET /api/app/football/player-ranks
GET /api/app/football/team-ranks
```

全部为公开只读接口，继续使用 `Result` 和 `PageResult`。

## 4. 赛季模型设计

赛季以 `(league_id, season_code)` 唯一，保存中文赛季名、起止日期、当前标记、来源记录和同步时间。同一联赛 Demo 数据只有一个 ACTIVE 当前赛季。

## 5. 阶段模型设计

阶段严格关联联赛和赛季，支持 `LEAGUE`、`GROUP`、`KNOCKOUT`、`FRIENDLY`。T15 Demo 使用 `LEAGUE` 或 `GROUP`，不实现淘汰赛树。

## 6. 积分榜计算和排序

积分为 `won * 3 + drawn - deduction_points`，净胜球为 `goals_for - goals_against`。统一 Demo 排序为积分、净胜球、进球数降序，再按球队 ID 升序。SQL 校验使用窗口函数重新计算期望排名。

## 7. 球员榜 rankType

支持 `GOALS`、`ASSISTS`、`YELLOW_CARDS`、`RED_CARDS`、`SHOTS`、`SHOTS_ON_TARGET`、`RATING`、`SAVES`、`APPEARANCES`、`MINUTES`。输入先解析为枚举，再映射内部固定列和固定次级排序，非法值返回 `40001`。

## 8. 球队榜 rankType

支持 `GOALS_FOR`、`GOALS_AGAINST`、`ASSISTS`、`YELLOW_CARDS`、`RED_CARDS`、`SHOTS`、`SHOTS_ON_TARGET`、`CORNERS`、`FOULS`、`CLEAN_SHEETS`、`AVG_RATING`。失球榜升序，其余默认降序，记录返回 `sortDirection`。

## 9. 稳定排名方式

积分榜使用落库且经一致性验证的连续排名；球员和球队榜按查询页偏移生成连续位置排名 `1,2,3...`。相同主指标使用固定次级字段和实体 ID 保证稳定，前端无需重排。

## 10. Demo 实际数量

全量 ResetDemo 基线为 16 赛季、16 阶段、40 积分榜记录、240 球员统计、40 球队统计。增量模式按当前数据库实际联赛和比赛归属补缺，不再为了达到固定数量把同一球队轮转到多个联赛；本机增量后为 22 赛季、22 阶段、55 积分榜记录、358 球员统计、55 球队统计。

## 11. 统计一致性

球员均存在且属于对应球队；出场不少于首发、射门不少于射正、进球不超过射正，非门将扑救为 0，评分位于 5.00–9.50。球队 played、进球和失球与积分榜逐条一致，助攻不超过进球，所有计数非负。

## 12. validate-demo-data 结果

新增赛季、阶段、积分榜、球员统计和球队统计校验项全部返回异常数 0；离线确定性、容量、表生成和 `source=DEMO` 检查全部 PASS。

## 13. N+1

未引入 N+1。积分榜一次批量读取球队；球员榜只查询当前页并分别批量读取球员和球队；球队榜只查询当前页并批量读取球队。`FootballRankPerformanceStructureTests` 验证 Mapper 调用次数不随记录数增长。

## 14. SQL 次数和接口耗时

本机开发环境单次实测：赛季 2 SQL/9ms、阶段 2 SQL/8ms、积分榜 5 SQL/26ms、球员榜 6 SQL/23ms、球队榜 5 SQL/17ms。额外 SQL 用于联赛、赛季和阶段作用域校验；榜单记录装配保持常数级。

## 15. mvn test

普通 `mvn test`：70 个测试，0 失败、0 错误，2 个依赖数据库凭据的编码测试按设计跳过。`check-t15.ps1` 注入开发数据库环境后：70 个测试，0 失败、0 错误、0 跳过。

## 16. check-t15.ps1

默认路径覆盖关键文件、既有数据快照、增量迁移/补数、逐 ID 指纹比对、测试、打包、T03-T15 smoke、进程停止和 8080/8090 残留检查，不再重置数据库。破坏性路径必须同时传入 `-ResetDatabase -ConfirmReset`，底层 `reset-dev-db.ps1` 还要求确认文本 `RESET south_stand`。

## 17. 增量与数据保护

`V015__season_standings_ranks.sql` 只创建缺失表和索引。`seed-t15-incremental.sql` 使用业务唯一键 `NOT EXISTS` 补缺，并从比赛出场次数推导球队唯一主联赛；不清理用户、内容、互动、文件、球队、球员或比赛数据。`check-existing-data-preserved.ps1` 对 14 张关键表保存 ID 和关键字段 SHA-256，After 阶段要求旧 ID 全部存在、行数不下降且指纹一致。本机首次快照覆盖 3970 条既有记录，增量执行后全部通过。

同一数据库连续执行两次 Incremental，结果均为：22 赛季、55 积分榜记录、358 球员统计、55 球队统计，第二次无重复增长。最终 `check-t15.ps1` 的 T03-T15 smoke 全部通过，smoke 后再次比对 3970 条旧记录仍一致，8080/8090 均无监听进程。

## 18. 遗留问题

当前数据为结构正确的确定性 Demo 数据，不是官方体育数据。真实联赛可能使用交锋、附加赛或供应商排名，未来应由 Provider 写入最终排名并保留现有查询契约。当前未实现淘汰赛树、实时同步或完整比赛技术统计。

## 19. git status --short

本轮未执行 `git add`、`commit` 或 `push`。工作区保留 T15 功能与本次增量保护相关修改，详见 `git status --short`。

```text
 M README.md
 M docs/02_REQUIREMENT_SCOPE.md
 M docs/05_DATABASE_SCHEMA.md
 M docs/06_API_SPEC.md
 M docs/11_VALIDATION_AND_SMOKE_GUIDE.md
 M docs/12_CODEX_TASK_PLAN.md
 M scripts/data/README.md
 M scripts/data/demo-config.json
 M scripts/data/generate-demo-data.py
 M scripts/data/validate-demo-data.py
 M scripts/sql/schema.sql
 M scripts/sql/seed-demo.sql
 M scripts/sql/validate-demo-data.sql
 M src/main/java/com/southstand/common/config/SecurityConfig.java
 M src/main/java/com/southstand/football/league/controller/LeagueController.java
?? docs/audits/T15_SEASON_STANDINGS_RANKS_REPORT.md
?? scripts/windows/check-t15.ps1
?? scripts/windows/smoke-football-ranks.ps1
?? src/main/java/com/southstand/football/rank/
?? src/test/java/com/southstand/football/rank/
```
