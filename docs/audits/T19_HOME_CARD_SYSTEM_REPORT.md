# T19 首页多类型卡片体系完成报告

## 1. 完成结论

T19 已将原始需求中的六类首页卡片收口到现有 `GET /api/app/feed?tab=recommend`。T18 先生成 CONTENT/MATCH 核心流并保留内部约 7:3、A/B 与三级降级；T19 再由 Java 规则稀疏插入 DISCUSSION、HOT_COMMENT、RANKING、PLAYER_RATING。未新增表、migration、seed 或 Demo 数据脚本，未实现转会、数据辩论场等范围外能力。

## 2. 修改和新增文件

- Feed 接入与兼容：`FeedService.java`、`FeedCardVO.java`、`RecommendationTargetType.java`。
- 配置和上下文：`HomeCardProperties.java`、`HomeFeedUserContext.java`、`application.yml`、`application-dev.yml`。
- 辅助卡服务：`AuxiliaryCardService.java`、`DiscussionCardService.java`、`HotCommentCardService.java`、`RankingCardService.java`、`PlayerRatingCardService.java`。
- 组合层：`HomeFeedCompositionService.java`。
- 强类型 payload：`HomeCardPayload.java` 及四类 payload。
- 测试：五个 T19 服务测试，并扩展 `FeedServiceTests`、`FeedPerformanceStructureTests`。
- 验收：`smoke-home-card-system.ps1`、`check-t19.ps1`。
- 文档：本报告及 `T19_HOME_CARD_REQUIREMENT_ALIGNMENT.md`。

## 3. 支持的 cardType 与数据来源

| cardType | 来源 | 算法版本 | 说明 |
| --- | --- | --- | --- |
| CONTENT | 现有 content | RULE_V2 / CF_V1 | 新闻、帖子、文章；T18 推荐不变 |
| MATCH | match_info | RULE_V2 | T18 核心混排不变 |
| DISCUSSION | POST + relation + interaction | AUX_RULE_V1 | 评论高权重、互动、时间衰减、主队/关注加权 |
| HOT_COMMENT | T12 comment hot formula | AUX_RULE_V1 | 复用 `calculateHotScore`，评论正文截断到 280 字 |
| RANKING | T15 三类统计表 | AUX_RULE_V1 | STANDING/POINTS、PLAYER/GOALS、TEAM/GOALS_FOR，各 Top 5 |
| PLAYER_RATING | T17 比赛球员统计及用户评分 | AUX_RULE_V1 | 仅 FINISHED；主队、关注球队、重要/最近比赛依次优先 |

`AUX_RULE_V1` 只标识辅助卡规则，不参与 T18 RULE_V2/CF_V1 A/B 比较。

## 4. 核心流、组合、比例与顺序

调用链为：Feed 候选批量装载 → T18 RecommendationService → CONTENT/MATCH core stream → AuxiliaryCardService → HomeFeedCompositionService → 最终分页。

- T18 core stream 内部仍保持约 7:3；辅助卡不送入 Python，也不改变 T18 candidate set。
- 辅助卡默认启用，`max-ratio=0.20`、`min-gap=3`，均可通过环境变量覆盖。
- 最大辅助卡数按 `floor(coreCount * ratio / (1-ratio))` 计算，保证最终占比不超过配置值。
- 每插入一张辅助卡前至少经过 3 张核心卡；候选不足时不造空卡。
- 所有排序都有 score、时间、ID 等稳定 tie-breaker；未使用随机数。

## 5. payload、cardId/cardKey 与 attribution

旧 CONTENT/MATCH 公共字段保持不变，新增可选强类型 `payload`、跨类型 `cardKey` 和卡片级 `algorithmVersion`。

- `CONTENT:{contentId}`
- `MATCH:{matchId}`
- `DISCUSSION:{contentId}`
- `HOT_COMMENT:{commentId}`
- `RANKING:{rankingType}:{rankType}:{leagueId}:{seasonId}`
- `PLAYER_RATING:{matchId}`

最终组合完成后统一重建连续 `position` 和唯一 `impressionId=requestId:cardKey`；页面级 T18 `algorithmVersion/modelVersion/experimentId/experimentBucket/requestId` 保留。

行为类型已允许 CONTENT、MATCH、COMMENT、RANKING、PLAYER_RATING。HOT_COMMENT 可记录为 COMMENT；PLAYER_RATING 可记录为 MATCH；RANKING 的结构维度应放入 `extraJson`，不向 BIGINT `targetId` 硬塞字符串。Python 训练/打分逻辑未修改，仍只读取和处理 CONTENT。

## 6. 去重与分页

- 先按 `cardKey` 做跨类型稳定去重。
- 若同一 `contentId` 同时入选 CONTENT 和 DISCUSSION，保留 DISCUSSION 并移除 CONTENT。
- 评论、榜单 slot 和赛后评分均由各自稳定 `cardKey` 去重。
- 组合在分页前完成，因此不会出现“分页后插卡”导致超出 pageSize。
- 真实 Smoke 验证 page1/page2 无重复 `cardKey`，position 连续，impressionId 非空且唯一。

## 7. 性能与 SQL

所有辅助服务使用有限候选集和批量查询：

- DISCUSSION：内容、关系、根评论、作者各固定一次批量查询。
- HOT_COMMENT：评论、父内容、作者各固定一次批量查询。
- RANKING：赛季/阶段和三类榜单固定查询，team/player 分别批量装载。
- PLAYER_RATING：最多 30 场已结束比赛，全部球员统计一次批量查询；选定比赛评分一次，player/team 批量装载。
- 用户主队和关注上下文只在 Feed 层加载一次并传给全部辅助服务。

SQL 次数随候选数量保持常数级；`FeedPerformanceStructureTests` 验证 40 条内容/评论仍为固定查询次数。未发现 T19 引入的 N+1。T14 原有批量装载未改写，Python 每次 Feed 请求仍最多调用一次。

## 8. 数据库与旧数据保护

- 新表：0。
- migration：0。
- T19 seed / Demo 增量：0。
- 未 reset 数据库，未修改 source 非 DEMO 数据。
- 两次 `check-t19.ps1` 均在执行前后校验既有 14 张关键表的行级指纹；第一次保护 4,888 条执行前数据，第二次保护 4,976 条执行前数据，关键字段全部未变。两次之间增长来自既有 Smoke 按原流程创建的验收用户/行为记录，不是 T19 seed。

## 9. 测试和验收结果

执行日期：2026-08-22（Asia/Shanghai）。

| 项目 | 结果 |
| --- | --- |
| T19 定向测试 | 17 项通过 |
| 完整 Java 测试 | 127/127 通过，0 failure，0 error，0 skipped |
| Python 测试 | 8/8 通过；仅有上游 Starlette/httpx 弃用警告 |
| `mvn -DskipTests clean package` | 通过 |
| T03–T18 回归矩阵 | 连续两轮通过 |
| T18 RULE_V2 bucket A | 连续两轮通过 |
| T18 CF_V1 bucket B | 连续两轮通过 |
| Python down → bucket B RULE_V2 | 连续两轮 HTTP 200 并通过 |
| T19 RULE / CF / fallback Smoke | 六次全部通过 |
| `check-t19.ps1` | 连续两次输出 `T19 check passed` |
| 端口清理 | 两轮结束后 8080/8100 均无监听 |
| `git diff --check` | 通过（仅 Git 的 LF→CRLF 提示，无 whitespace error） |

## 10. 搜索、发布、前端与延期项

- 搜索后端已支持 TEAM/PLAYER/MATCH 首页入口，尚不支持 CONTENT 搜索；不阻塞首页 Feed 第一版，若产品确认需要内容搜索应单独排期。
- 帖子和文章发布接口已具备，发布内容复用现有内容源进入 Feed。
- Flutter 尚需实现四类新 payload 的组件、跳转和埋点映射。
- 明确延期/不做：TRANSFER、转会中心、数据辩论场、比赛事件投票、视频集锦、裁判评分、复杂杯赛树。

## 11. 最终验收结论

原始首页核心需求是否已对齐：是  
CONTENT 是否正常：是  
MATCH 是否正常：是  
DISCUSSION 是否可用：是  
HOT_COMMENT 是否可用：是  
RANKING 是否可用：是  
PLAYER_RATING 是否可用：是  
资讯是否仍保持首页主体：是  
T18 7:3 核心流是否保留：是，仅指 core stream 内部比例  
CF_V1 是否仍只处理 CONTENT：是  
Python 关闭 Feed 是否正常：是，B bucket 降级 RULE_V2 且 HTTP 200  
是否存在重复卡片：验收范围内否  
是否存在 N+1：未发现 T19 引入的 N+1  
旧数据是否保留：是  
T03-T18 是否全部回归：是  
T19 Smoke 是否通过：是  
check-t19 是否连续两次通过：是  
是否存在阻塞第一版前端的首页后端缺口：否；仍需 Flutter 渲染工作  
是否可以 Git 提交：可以，但本轮未执行 add/commit/push  

## 12. 当前 Git 状态

工作区仅包含本轮 T19 的源码、配置、测试、脚本和文档改动；未执行 `git add`、`git commit` 或 `git push`。具体文件清单以交付时 `git status --short` 为准。

