# T18 推荐系统融合完成报告

完成时间：2026-08-22（Asia/Shanghai）  
基线：`main` / T17，未复制同事后端、未覆盖 FeedService、未执行同事迁移、未 Git 提交。

## 1. 修改与新增文件

- Java 推荐模块：`src/main/java/com/southstand/recommend/**`，包含配置、模型、规则策略、实验、混排、远端客户端、行为、指标、Controller、Entity/Mapper/VO。
- Feed 增量接入：`FeedService`、`FeedCardVO`、`FeedPageResult`；保留 T14 的 content/match batch loader 和原有构造兼容。
- 互动闭环：`InteractionService`、`CommentService`；仅业务成功后写 LIKE/FAVORITE/COMMENT，日志失败不影响事务结果。
- 安全与配置：`SecurityConfig`、`application.yml`、`application-dev.yml`。
- 数据：V018、6060 行增量 Seed 及生成器。
- Python：`recommend-service/app/**`、三组测试、requirements、README、`.env.example`。
- 验证：start/stop/smoke/check 脚本及 8 个 T18 Java 测试类（含策略、实验、混排、远端、行为、指标、降级、迁移）。
- 文档：README 和指定的需求、技术、架构、数据库、API、安全、部署、验证、任务、数据文档。

## 2. 最终架构与职责边界

链路为 `FeedController -> FeedService 原过滤/T14 批量加载 -> RecommendationService -> RULE/CF -> diversity -> 7:3 mix -> Feed VO`。Java 拥有候选、权限、MATCH、RULE_V2、实验、降级、理由、归因和最终混排；Python 只 SELECT `user_behavior_log`，只计算 CONTENT Item-CF，不读用户/内容/比赛等核心表，不写数据库。

每个 Feed 请求最多一次 Python 请求；推荐增加一次批量近期 EXPOSE 查询，无逐卡 Python、作者、关系、点赞、收藏或行为查询。`FeedPerformanceStructureTests` 和全量回归通过，未引入 N+1。

## 3. 保留的同事业务逻辑与适配改进

保留 RULE_V2 主体因子、Item-CF 余弦思想、行为权重、50/50 A/B、中文 reason、约 7:3 混排、行为闭环和 `CF -> RULE -> HOT` 降级意图。

适配改进包括：CF 只训练 CONTENT；分数严格归一化到 `[0,1]`；每物品 Top-K=50；user-item 累计封顶 8；14 天指数时间衰减；强互动内容乘 0.15 再推荐系数；Python 不再实现 RULE/MATCH/最终 Feed；空模型、非法 ID/score/version/JSON/HTTP/timeout 全部降级；reload 新模型成功后原子替换，失败保留旧模型；下发不伪造 EXPOSE。

## 4. RULE_V2 因子与权重

内容：主队 +50、关注作者 +45、关注球队 +35、关注球员 +25；互动原始值 `LIKE*2 + COMMENT*3 + FAVORITE*4` 经 `log1p` 压缩并封顶 30；发布时间 `30*exp(-hours/24)`；6 小时内且零互动新内容 +10；近 3 天真实曝光 -100。比赛：LIVE +80；SCHEDULED `40 + 20*exp(-hoursUntil/48)`；FINISHED 战报 `25*exp(-hoursSinceEnd/48)`；重要比赛 `50 + level*5`；主队 +50、关注球队 +35。稳定排序为 score DESC、时间 DESC、id ASC。

理由优先体现主队、作者、球队、球员、相似内容、直播/焦点/即将开始、新鲜和热门。Card 已返回 `reasonCode/reason`。

## 5. Item-CF

行为权重：CLICK 1.0；DETAIL `1.5 + min(1,dwellMs/60000)`；LIKE 2.0；FAVORITE 2.5；COMMENT 3.0；EXPOSE 0 且不进入正反馈矩阵。正反馈再乘 `exp(-days/14)`。

算法建立 user-item 权重、item-user 倒排及 item cosine，相似邻居每项仅保留 50。候选分数使用“历史权重占上限比例 × similarity”的有界加权平均，再夹紧到 `[0,1]`；Java 使用 `ruleScore + 35*cfScore`。重复行为因 user-item 上限 8 不会无限放大。

## 6. Python API 与运行状态

- `GET /health`
- `GET /api/internal/recommend/stats`
- `POST /api/internal/recommend/content-scores`
- `POST /api/internal/recommend/reload?days=30`

确定性 Seed 的实测模型规模：20 用户、120 CONTENT、3060 条正反馈；构建约 120 ms；四候选评分本机约 5 ms。MySQL 不可用时进程仍启动且 `modelReady=false`。部署前仍需创建只对 `user_behavior_log` 有 SELECT 权限的专用账号。

实际环境初检发现 FastAPI 等依赖并未安装，与 Prompt 的“已安装”描述不一致；只安装了仓库 `requirements.txt` 指定依赖，没有升级 Python 或修改 PATH。

## 7. A/B、混排、降级与效果

分桶使用 SHA-256(`REC_HOME_V1:userId`)；匿名固定 A，默认 50%。20 个 Demo 用户实测 A/B 为 10/10，重复请求 bucket 不变。B 用户 Python 失败后算法标记为 RULE_V2，但 bucket 仍为 B。

Java 按 7 CONTENT + 3 MATCH 的块进行近似 7:3 混排，候选不足自动补齐；轻量 look-ahead 避免连续 3 条同作者，并抑制短窗口相同 matchId。比赛从不进入 CF。

实测两个不同兴趣的 B 用户 Top10 只重合 2 条；同用户重复 Top10 完全稳定；page1/page2 重合 0。Demo 请求观察值：RULE Feed 34–48 ms，CF Feed 42–62 ms，Python 独立调用约 5 ms，Python down fallback 44 ms。CF/RULE 的 Top10 因 A/B 用户本身偏好不同，不把该观察当作严格离线实验结论。

降级验证：Python 在线 B -> CF_V1；Python 关闭 B -> HTTP 200 / RULE_V2 / bucket B；RULE 异常 -> HOT_V1 且不再调用 CF。远端客户端还覆盖 500、modelReady=false、越界 ID/score 和连接失败，并在连续 3 次失败后冷却 30 秒。

## 8. V018、attribution 与行为闭环

V018 包含要求的全部字段、`client_event_id` 唯一键及五组联合索引。Feed 页返回 algorithm/model/experiment/bucket/request；卡片返回 impression/position/reason。行为 API 每次最多 100 条，返回 received/saved/duplicated/rejected；同 clientEventId 重试幂等。DETAIL 可完整携带 scene、算法、模型、实验、request、impression、position、dwellMs。

LIKE/FAVORITE 仅在激活成功时、COMMENT 仅在创建成功时由现有业务服务安全记录。CLICK/DETAIL 由 Flutter 上报；EXPOSE 必须由卡片真实进入可视区后上报，后端只生成 attribution。本轮未修改 Flutter。

Seed 为 6060 条：EXPOSE 3000、CLICK 1500、DETAIL 1000、LIKE 300、FAVORITE 160、COMMENT 100；覆盖 20 用户、120 CONTENT、最近 29 天、四个兴趣簇，Seed 中 A/B 各 3030。固定 `t18-demo-*` 和 `INSERT IGNORE`，多次执行仍为 6060。

## 9. 指标、安全与性能

管理员接口 `/api/internal/recommendation/metrics` 支持 scene、algorithmVersion、experimentId、bucket 和时间，输出曝光、点击、CTR、详情/停留、互动率及进程内 CF 请求/成功/降级/延迟。`/api/internal/**` 已限制 ADMIN；Python 绑定 127.0.0.1。未提交真实 `.env`、密码、JWT、Token 或 Key。

Feed SQL 次数随候选量保持常数级：沿用 T14 固定批量查询集合，T18 仅增加一次曝光历史批量查询；CF 每请求至多一次 HTTP。结构测试与实际全量 Smoke 未发现 N+1。精确 SQL 条数会随 tab 和登录态不同，因此不使用一个误导性的单值。

## 10. 验证结果

- Python：8 passed；有 1 条 Starlette TestClient/httpx deprecation warning，不影响功能。
- Java：117 tests，0 failures，0 errors，0 skipped。
- `mvn clean package`：成功。
- T03–T17 Smoke：全部通过，并在完整 `check-t18` 两次运行中各自再次通过。
- T18 Smoke：RULE(A)、CF(B)、Python down fallback(B)、六行为保存及 clientEventId 重试判重全部通过。
- `check-t18.ps1`：完整连续执行两次通过；每次含指纹、迁移、Seed、Python tests/model、mvn test/package、T03–T17、T18 RULE/CF/fallback、最终指纹与端口清理。
- 旧数据：第一次保护 4760 个既有指纹行，第二次保护包含前次合法 Smoke 增量的 4828 行，均无删除或关键字段变化。
- 端口：最终 8080/8100 均无监听。

## 11. 遗留问题

1. Flutter 仍需实现真实可视 EXPOSE、CLICK/DETAIL attribution 上报。
2. 生产部署需创建 recommendation 专用只读 DB 用户，并为 Python 内部 API 增加网络层访问控制。
3. 当前分页依赖稳定 tie-breaker 和 5 分钟曝光隔离，未实现跨长时间的 snapshot cursor。
4. CF 模型为单进程内存模型；多实例部署需统一模型版本/重建调度。
5. 清理 Python 测试依赖升级时可处理现有 Starlette deprecation warning。

## 12. 最终结论

旧数据是否保留：是  
RULE_V2 是否可用：是  
CF_V1 是否可用：是  
Python 关闭时 Feed 是否可用：是  
三级降级是否可用：是  
A/B 是否稳定：是  
行为闭环是否可用：是  
推荐归因是否完整：后端完整；Flutter 真实 EXPOSE/CLICK/DETAIL 待接入  
7:3 混排是否可用：是  
是否存在 N+1：未发现  
推荐效果是否体现个性化：是  
T03-T17 是否全部回归：是  
T18 Smoke 是否通过：是  
check-t18 是否连续两次通过：是  
是否存在未解决阻塞：否  
是否可以 Git 提交：技术验收已通过，可由项目负责人审阅后提交；本轮未自动提交

## 13. git status --short 摘要

工作区包含本次 README/docs、Feed/互动/安全/config 修改，以及未跟踪的推荐 Java 模块、Python 服务、V018、Seed/生成器、Windows 脚本、T18 测试和两份审计报告。未执行 `git add`、`commit` 或 `push`。
