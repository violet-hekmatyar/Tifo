# T20 足球详情页 Backend Contract 对齐报告

## 1. 结论与审计范围

T20 已按《tifo需求文档》球队、球员、比赛详情章节完成第一版后端契约收口。实现只聚合 T15、T16、T17 及既有内容关系数据：0 新表、0 migration、0 seed，未重置或改写既有业务数据，也未实现转会中心、实时比分、视频、高级战术图或裁判评分。

| 审计项 | 需求 | 开发前状态 | 已有接口/数据 | 开发前缺口 | 是否阻塞 Flutter | T20 结果 |
| --- | --- | --- | --- | --- | --- | --- |
| 球队详情 | 总览、帖子、球员、数据、赛程、排名、荣誉、队内榜单 | T16 已有总览、阵容、统计、荣誉 | `football_team`、比赛、`content + content_relation`、T15 球员赛季统计 | 无完整赛程分页、帖子分页；总览只有单项排名和部分榜单 | 是 | 补齐赛程、内容、全赛事排名、荣誉和 4 类榜单 |
| 球员详情 | 总览、帖子、数据、比赛、生涯、俱乐部、国家队、状态 | T16 已有 overview/stats/teams/career | `football_player.retired`、`team_player`、T17 出场和比赛球员统计 | 无比赛记录与帖子契约；总览未明确退役/俱乐部/国家队 | 是 | 补齐比赛、内容、状态和球队兼容字段 |
| 比赛详情 | 评分、总览、阵容、排名、统计、事件 | T17 已有分散的详情、阵容、统计和评分接口 | 比赛、事件、阵容、球队统计、球员统计、评分、积分榜 | 无单一聚合 overview；无主客队排名契约 | 是 | 新增聚合接口及主客队排名 |

## 2. 需求对应关系与完成度

### 球队详情：第一版完成

- `overview` 保持原字段并兼容新增：所有参赛赛事的积分信息、近 10 条关联内容、荣誉、进球/助攻/出场/评分四类队内榜单。
- 球员 Tab 继续复用 T16 阵容分页；数据 Tab 继续复用 T16 按赛季/赛事统计；赛程 Tab 新增球队比赛分页。
- 帖子 Tab 从 `content_relation(relation_type=TEAM)` 批量关联 `content`，不复制内容，兼容 `POST/ARTICLE/NEWS/REPORT` 既有类型。
- 赛程包含时间、联赛、主客队、队徽、比赛状态和比分；即将比赛升序，其他状态倒序。

### 球员详情：第一版完成

- `overview` 兼容新增 `retired`、`playerStatus`、`club`、`nationalTeam` 和 `recentMatches`，原有基本信息、赛季数据、生涯字段不删除。
- 比赛记录复用 T17 `football_match_player_stat` 与出场数据，返回比赛时间、主客队、所属球队、首发状态、分钟、进球、助攻和官方评分。
- 帖子 Tab 复用 `content_relation(relation_type=PLAYER)`，空数据返回空分页数组。
- 俱乐部/国家队复用 `team_player.team_type`。当前没有国家队关系时 `nationalTeam=null`，未创建复杂国家队子系统。
- 退役球员直接复用 `football_player.retired`；退役时状态为 `RETIRED` 且近期比赛为空。

### 比赛详情：第一版完成

- 新 overview 聚合原比赛详情（含事件）、T17 阵容、球队统计、球员统计、用户评分和 T15 积分榜。
- 排名同时返回主、客队的名次、场次、胜平负、进失球、净胜球和积分，以及联赛/赛季/阶段信息。
- 事件继续复用现有模型与接口，可承载进球、黄牌、红牌、换人和 VAR；未新增裁判评分。
- 当前数据库没有“赛前积分榜快照”。因此返回值明确标注 `snapshotType=CURRENT_STANDING`；无法定位赛季时为 `UNAVAILABLE`，不会把当前排名伪装为历史赛前排名。

## 3. 新增及扩展接口

| 方法与路径 | 用途 |
| --- | --- |
| `GET /api/app/football/teams/{teamId}/matches` | 球队赛程/赛果，支持 `status`、`pageNum`、`pageSize` |
| `GET /api/app/football/teams/{teamId}/contents` | 球队关联内容，支持 `contentType` 和分页 |
| `GET /api/app/football/players/{playerId}/matches` | 球员比赛记录，支持分页 |
| `GET /api/app/football/players/{playerId}/contents` | 球员关联内容，支持 `contentType` 和分页 |
| `GET /api/app/football/matches/{matchId}/overview` | 比赛详情聚合：比赛/事件、阵容、统计、评分、排名 |
| `GET /api/app/football/teams/{teamId}/overview` | 原接口兼容新增全赛事排名、完整榜单、荣誉 |
| `GET /api/app/football/players/{playerId}/overview` | 原接口兼容新增状态、俱乐部/国家队和近期比赛 |

所有响应均为 VO；新增字段为兼容性追加，空集合使用空数组/空分页，缺失单值使用 `null`。

## 4. 修改文件

生产代码：

- `src/main/java/com/southstand/football/detail/service/FootballDetailService.java`
- `src/main/java/com/southstand/football/detail/service/MatchOverviewService.java`
- `src/main/java/com/southstand/football/detail/vo/FootballDetailVO.java`
- `src/main/java/com/southstand/football/detail/vo/MatchOverviewVO.java`
- `src/main/java/com/southstand/football/team/controller/FootballTeamController.java`
- `src/main/java/com/southstand/football/player/controller/FootballPlayerController.java`
- `src/main/java/com/southstand/football/match/controller/FootballMatchController.java`

测试与门禁：

- `src/test/java/com/southstand/football/detail/FootballDetailTestFixture.java`
- `src/test/java/com/southstand/football/detail/TeamDetailServiceTests.java`
- `src/test/java/com/southstand/football/detail/PlayerDetailServiceTests.java`
- `src/test/java/com/southstand/football/detail/MatchDetailServiceTests.java`
- `scripts/windows/smoke-detail-page-contract.ps1`
- `scripts/windows/check-t20.ps1`
- `docs/audits/T20_DETAIL_PAGE_ALIGNMENT_REPORT.md`

## 5. 数据库变化

- 新表：0。
- migration：0。
- seed：0。
- 缓存详情表：0。
- T20 接口为只读实时聚合；两轮门禁前后数据指纹验证均通过。

## 6. Flutter 使用说明

- 球队页首屏调用球队 `overview`；球员、数据、赛程和帖子 Tab 分别按需调用既有阵容/统计及新增 matches/contents，避免首屏加载过重。
- 球员页首屏调用球员 `overview`；完整比赛和帖子分页使用新增接口；`nationalTeam` 必须按可空字段处理；`retired=true` 时可隐藏比赛 Tab。
- 比赛页可一次调用比赛 `overview` 渲染五个 Tab；`playerStats` 本身是分页对象。如需独立刷新，仍可使用 T17 原接口。
- 排名 UI 应读取 `ranking.snapshotType`：`CURRENT_STANDING` 显示“当前排名”，`UNAVAILABLE` 隐藏排名区域，不能标注成赛前历史排名。

## 7. 性能检查

- 球队赛程、内容、赛事排名、榜单均先取有界 ID 集合，再批量加载球队、联赛、赛季、球员和内容。
- 球员比赛先批量取 T17 比赛球员统计/出场，再批量取比赛、球队、联赛；没有逐场查询。
- 比赛排名一次取主客队积分记录并批量取两队信息。
- 新增服务测试使用 mapper 调用次数验证关键批量查询；SQL 数量不随返回球员、比赛或内容条数线性增长。
- 所有内存分页的候选集合上限为 200，接口 `pageSize` 继续受统一分页约束。

## 8. 测试结果

- `TeamDetailServiceTests`：基本信息、阵容依赖、赛程、内容、数据、榜单、批量查询和空内容通过。
- `PlayerDetailServiceTests`：overview、stats、career、matches、club、national team、retired 和批量查询通过。
- `MatchDetailServiceTests`：overview、lineup、stats、ranking、rating、events、空排名兼容和批量查询通过。
- Java：134 tests，0 failures，0 errors，0 skipped。
- Python 推荐服务：8 tests passed（仅有上游 Starlette/httpx2 弃用提示）。
- T03–T19 全量回归通过；RULE、CF、fallback 路径均通过。
- `scripts/windows/check-t20.ps1` 连续两次通过；每次均验证 T20 契约、数据指纹、无 migration/seed，并确认 8080/8100 无残留监听。

## 9. 遗留问题

- 精确“赛前排名”需要历史积分榜快照，当前模型只能诚实返回当前排名。这不阻塞 Flutter 第一版，但 UI 文案必须按 `snapshotType` 展示。
- 国家队仅提供 `team_player.team_type` 兼容能力；没有关系数据时返回 `null`。国家队赛事/阵容的完整建模延期，不阻塞可空展示。
- 原需求文档还出现解围、抢断类队内榜单；现有 T15 赛季聚合只稳定覆盖 T20 明确要求的进球、助攻、出场、评分。解围/抢断榜需先增加可信的赛季聚合来源，第一版暂不提供。
- 现有比赛模型没有裁判基础字段，T20 按范围约束未增加裁判或裁判评分。

## 10. Git 状态

未执行 `git add`、`git commit` 或 `git push`。当前改动可在人工审阅通过后提交。
