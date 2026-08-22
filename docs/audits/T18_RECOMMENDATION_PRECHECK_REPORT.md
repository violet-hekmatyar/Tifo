# T18 推荐系统融合开发前专项审计报告

> 审计日期：2026-08-21  
> 正式开发基线：`D:\Football-APP`  
> 同事参考目录：`C:\Users\hekmatyar\Desktop\推荐系统改动说明`  
> 审计性质：只读调查、环境验证与方案建议；未实现推荐业务、未执行 migration、未修改数据库数据、未安装依赖、未提交 Git。  
> 敏感信息处理：数据库密码、JWT Secret 等仅核验“已配置/未配置”，未写入本报告。

## 0. 执行摘要

结论：**当前部分具备 T18 开发条件**。

- T17 后端主线已提交，Git 基线清晰且审计开始时工作区干净。
- JDK、Maven、MySQL、Redis、Flutter 均可用；MySQL `south_stand` 可只读查询，Redis `PING` 成功。
- 本机有 Python 3.13.13 和 `py` launcher，但系统 Python 未安装 FastAPI、Uvicorn、Pydantic、PyMySQL。
- 同事目录中的 `.venv` 来自另一台机器，记录 Python 3.12.14，但其基础解释器路径已失效，因此不能直接运行。
- 8100 端口当前空闲，可继续作为推荐服务端口。
- 当前主线最后一个 migration 是 `V017__match_lineups_stats_ratings.sql`；同事用 `V017` 创建行为表，存在直接编号冲突。T18 应重新设计为新的 `V018`，不能复制执行同事的 V017/V018。
- 同事 Python 服务已经采用“Spring Boot 提供候选集，Python 对候选评分/排序”的模式，但 Python 同时重复实现 RULE_V2 和 7:3 混排，职责边界过重。T18 建议 Java 负责候选、规则、比赛排序和最终混排，Python 初版只返回 CONTENT 的 CF 分数或 rerank 结果。
- 最大风险不是算法本身，而是行为归因数据不足：缺少 `experiment_id`、`experiment_bucket`、`request_id`、`impression_id`、结构化 `position`、`client_event_id` 和毫秒级停留时长，无法可靠串联一次 Feed 请求、曝光、点击和详情停留。

## 1. 当前 Git / T17 基线

### 1.1 Git 状态

| 项目 | 审计结果 |
|---|---|
| 当前分支 | `main` |
| 最新提交 | `8917565 feat: complete T17 match lineups stats and player ratings` |
| 前五个提交 | T17、T16、T15、T14、T13 均有独立提交 |
| 审计开始时工作区 | 干净，`git status --short` 无输出 |
| T17 是否已提交 | 是 |

### 1.2 当前推荐能力

当前正式主线没有真正的 T18 推荐模块：

- `com.southstand.recommend` 只有空的 `package-info.java`。
- 没有 `RecommendationService`、远程算法客户端、A/B Router、行为实体/Mapper、算法版本字段或推荐配置。
- 数据库脚本中没有真实创建 `recommend_result` 或 `user_behavior_log`，仅文档保留了 P1 占位说明。
- 当前 `tab=recommend` 是 T07 规则排序，不是协同过滤推荐。
- 当前没有推荐曝光、点击、详情停留的归因闭环。

因此 T18 应从当前 T17 主线重新融合实现，不能把同事目录视为可合并分支。

## 2. 当前 Feed 架构

### 2.1 当前调用链

```text
FeedController
  -> FeedService.feed(tab, leagueId, teamId, pageNum, pageSize, cursor)
     -> currentUserContext()
        -> user_profile（主队）
        -> follow_record（球队/球员/用户关注）
        -> match_info（关注球队对应联赛）
     -> contentCards() / matchCards() / mixedCards() / followingCards()
        -> 查询最多 100 个 Content / MatchInfo 候选
        -> loadContentBatch() / loadMatchBatch() 批量补齐展示与评分数据
        -> toContentCard() / toMatchCard()
        -> contentScore() / matchScore()
     -> deduplicate()
     -> 全局 score 倒序
     -> page() 内存分页
     -> FeedPageResult
```

### 2.2 候选、排序、混排和分页

| 问题 | 当前实现 |
|---|---|
| 内容候选 | `contentCards()` 查询 `content`，按 `hot_score`、`publish_time` 倒序，最多 100 条 |
| 比赛候选 | `matchCards()` 查询 `match_info`，按 `important_level` 倒序、`match_time` 正序，最多 100 条 |
| 个性化候选 | 由主队、关注球队、关注球员、关注作者关联出的内容/比赛 ID 过滤 |
| 内容评分 | hotScore + LIKE×2 + COMMENT×3 + FAVORITE×4 + 阶梯时间分 + 关注加分 |
| 比赛评分 | LIVE 80 / SCHEDULED 40 / FINISHED+战报 25 + 重要级别 + 主队/关注球队加分 |
| 7:3 混排 | **不存在**；内容和比赛合并后统一按分数排序 |
| 去重 | `cardId` 去重，保留分数较高者 |
| 分页 | 排序完成后内存 `subList`；页大小上限 100 |
| cursor | Controller 接收，但当前 `FeedService` 未使用 |

### 2.3 T14 批量加载与 N+1 约束

必须保留的批量能力：

- 内容关联一次批量查询并按 `content_id` 分组。
- 球队、球员、比赛、联赛使用 `selectBatchIds`。
- 评论一次批量查询，在内存选每条内容的热门评论。
- 作者资料一次批量查询。
- 当前用户点赞/收藏状态分别一次集合查询，禁止逐卡 `selectCount`。
- 比赛战报、事件一次批量查询；事件涉及球员再统一批量加载。
- `FeedPerformanceStructureTests` 明确约束 40 条内容仍使用有界批量查询，不允许每卡查询作者、关系、点赞或收藏。

### 2.4 最佳推荐接入点

最佳插入层是：**候选查询和批量特征补齐之后，最终去重/排序/混排/分页之前**。

建议链路：

```text
FeedController
  -> FeedService（只做业务编排）
     -> Content/Match 候选查询
     -> 保留 T14 Batch Loader
     -> 组装轻量 RecommendationCandidate
     -> RecommendationService
        -> ExperimentService 选策略
        -> RULE_V2 或 CF_V1（失败回 RULE_V2）
     -> Match 规则结果 + Content 推荐结果统一混排
     -> VO 回填 reason / attribution
     -> 稳定分页
```

不建议让 `FeedService` 自己承担 HTTP 调用、A/B 哈希、行为落库、指标聚合或全部算法公式。它只应掌握业务候选、调用推荐门面、把推荐结果映射回 Feed VO。

### 2.5 绝对不能被覆盖的现有逻辑

- T17 的比赛阵容、球队/球员技术统计、用户球员评分代码与 SQL。
- 当前 `schema.sql`、`seed-demo.sql` 和 T15/T16/T17 incremental 数据链。
- T14 的 `loadContentBatch()`、`loadMatchBatch()` 及其性能结构测试。
- 当前 Feed 的状态过滤、联赛/球队过滤、关注候选语义、点赞/收藏状态和热门评论展示。
- 当前安全配置、统一 `Result`、分页上限和已有测试夹具。

## 3. 同事推荐目录结构

排除 `.git`、IDE 配置、`node_modules`、`target`、`build`、`dist`、`__pycache__`、`.venv/venv` 后，共审计 826 个文件：参考说明 1、Python 服务 8、前端 358、后端 459。

```text
推荐系统改动说明/
├── 推荐系统改动说明.md
├── recommend-service/
│   ├── README.md
│   ├── requirements.txt
│   └── app/
│       ├── __init__.py
│       ├── main.py
│       ├── loader.py
│       ├── cf.py
│       ├── scoring.py
│       └── selftest.py
├── Tifo-main/Tifo-main/
│   ├── pom.xml
│   ├── docs/
│   ├── scripts/
│   │   └── sql/
│   │       ├── migrations/
│   │       │   ├── V015__season_standings_ranks.sql
│   │       │   ├── V016__team_roster_player_career.sql
│   │       │   ├── V017__recommend_behavior_tables.sql
│   │       │   └── V018__behavior_log_algorithm_version.sql
│   │       └── seed-t17-behavior-demo.sql
│   └── src/
│       ├── main/java/com/southstand/
│       │   ├── card/                         # FeedService 与 Feed VO 被改动
│       │   └── recommend/                    # 20 个推荐主代码文件
│       │       ├── controller/
│       │       ├── dto/
│       │       ├── entity/
│       │       ├── mapper/
│       │       ├── service/
│       │       ├── vo/
│       │       ├── RecommendService.java
│       │       ├── RuleRecommendServiceImpl.java
│       │       ├── RemoteRecommendClient.java
│       │       ├── AbTestRouter.java
│       │       └── CardMixService.java
│       ├── main/resources/application*.yml
│       └── test/java/com/southstand/recommend/ # 5 个推荐测试文件
└── Tifo-front-main/Tifo-front-main/
    ├── apps/admin/                           # 未接入推荐功能
    └── apps/mobile/
        ├── lib/core/behavior/behavior_reporter.dart
        ├── lib/features/feed/
        │   ├── data/
        │   ├── domain/
        │   └── presentation/
        │       ├── controllers/feed_controller.dart
        │       └── widgets/{reason_chip,content_card,match_card,feed_card_renderer}.dart
        ├── lib/features/content/presentation/pages/content_detail_page.dart
        └── test/
            ├── core/behavior/behavior_reporter_test.dart
            └── features/feed/feed_controller_test.dart
```

未发现 Python `pyproject.toml`、`setup.py`、`Pipfile`、`environment.yml`、Dockerfile 或 docker-compose 文件。Python 服务的环境声明只有 `requirements.txt` 和 README。

## 4. 同事推荐功能组成

同事实现分为五块：

1. Python FastAPI Item-CF 服务。
2. Spring Boot 推荐抽象、RULE_V2、远程客户端和 A/B Router。
3. Feed 候选重构、7:3 混排、推荐理由和算法版本返回。
4. `user_behavior_log`、批量行为上报和指标聚合。
5. Flutter 推荐理由展示及 EXPOSE/CLICK/DETAIL 埋点。

同事后端不是当前 T17 完整快照：其目录缺少当前主线的 T17 migration、T17 incremental seed、`football.matchdata` 服务/实体和 T17 migration 测试；其 `schema.sql` 也与当前主线不同。因此即使推荐代码局部可参考，也绝不能整体复制或覆盖。

## 5. Python 实际版本要求

| 来源 | 结论 |
|---|---|
| 代码语法最低版本 | Python 3.10（使用 `X | None` 联合类型；`list[...]` 本身要求 3.9） |
| 同事 `.venv/pyvenv.cfg` | Python 3.12.14 |
| 同事 README | 只写 `python -m venv`，未声明明确版本 |
| 本机系统 Python | 3.13.13 |
| T18 推荐版本 | **Python 3.12.x（建议与真实参考环境一致）** |

推荐 3.12 而不是直接用 3.13 的原因：同事实际开发环境是 3.12.14；当前依赖对 3.12 的成熟度更确定；可减少 Pydantic/FastAPI 二进制依赖兼容差异。代码层面没有必须使用 3.12 的特性。

## 6. Python 依赖审计

| 依赖 | requirements 声明 | 代码真实使用 | 文档声明 | 备注 |
|---|---:|---:|---:|---|
| FastAPI | `>=0.110` | 是，`main.py` import | 是 | Web 框架 |
| Uvicorn | `>=0.29` | 启动命令使用；源码不 import | 是 | ASGI Server |
| Pydantic | `>=2.6` | 是，且使用 `model_dump()` | 是 | 明确要求 v2 |
| PyMySQL | `>=1.1` | 条件 import；仅 DB_HOST 配置后使用 | 是，可选 | MySQL 驱动 |
| NumPy/Pandas/SciPy/scikit-learn | 否 | 否 | 否 | 算法完全用标准库实现 |
| implicit/surprise | 否 | 否 | 否 | 未使用推荐算法库 |

失效 `.venv` 的 metadata 显示曾安装 FastAPI 0.141.1、Uvicorn 0.52.4、Pydantic 2.13.4、PyMySQL 1.2.0，但该环境不可执行，只能作为版本线索。`requirements.txt` 仅设下限、没有锁定上限或哈希，复现性不足，T18 落地时应生成受控版本约束。

## 7. Python 服务启动方式

参考实现启动入口：

```powershell
uvicorn app.main:app --host 0.0.0.0 --port 8100
```

- App 入口：`app.main:app`
- Host：`0.0.0.0`
- Port：`8100`
- 生命周期：FastAPI startup 时同步执行 `model.rebuild()`。
- 配置方式：直接读取环境变量 `DB_HOST`、`DB_PORT`、`DB_USER`、`DB_PASSWORD`、`DB_NAME`、`BEHAVIOR_SEED_SQL`；未使用 `.env` loader 或配置类。

当前未能启动既有服务：系统 Python 缺四个服务依赖，同事 `.venv` 又因基础解释器路径失效而不能运行。本轮遵守限制，没有安装依赖或创建新 venv。

## 8. Python API 审计

### 8.1 API 清单

| Method | Path | 输入 | 输出 | 是否在请求时查 DB |
|---|---|---|---|---:|
| GET | `/health` | 无 | `status`、`algorithmVersion`、模型统计 | 否 |
| GET | `/api/internal/recommend/stats` | 无 | users、itemsWithNeighbors、behaviorLogs、builtAt | 否 |
| POST | `/api/internal/recommend/feed` | RecommendRequest | RecommendResult | 否 |
| POST | `/api/internal/recommend/cards` | 同 feed | 同 feed | 否 |
| POST | `/api/internal/recommend/teams` | 同 feed，当前只是别名 | 同 feed | 否 |
| POST | `/api/internal/recommend/players` | 同 feed，当前只是别名 | 同 feed | 否 |
| POST | `/api/internal/recommend/matches` | 同 feed，当前只是别名 | 同 feed | 否 |
| POST | `/api/internal/recommend/reload?days=30` | query 参数 days | success + model stats | **是**，同步重建 |

### 8.2 recommend 请求

主要字段：

- `userId`：可选；缺少时 CF 分为 0。
- `scene`、`pageNum`、`pageSize`。
- `userProfile`：主队、关注球队/球员/作者。
- `candidateItems`：由 Java 传入完整候选及规则特征，包括目标类型/ID、作者、关联球队/球员/比赛、互动数、热度、发布时间、比赛状态/时间/重要度等。
- `exposedTargetKeys`：用于候选扣 100 分。

响应包含 `success`、`scene`、`algorithmVersion=CF_V1`、`items[{cardType,targetType,targetId,score,reason}]`、`fallback` 和错误字段。

### 8.3 当前属于 A 还是 B

结论：**当前已经是 B：Java 提供候选，Python 只在给定候选中评分/排序。** Python 不读取内容、比赛、球队或关注业务表来自己生成候选。

但当前 Python 不只是 CF：它还重复计算 RULE_V2、曝光降权和 7:3 混排。改造成目标边界的工作量不大，主要是：

- 请求保留 `userId + CONTENT candidateIds/keys`，必要时保留少量调试上下文。
- Python 只返回 `targetType + targetId + cfScore` 或排序后的 CONTENT keys。
- RULE_V2、MATCH 规则和最终混排统一留在 Java，避免两端权重漂移。

### 8.4 异常行为

- Pydantic 请求格式错误：FastAPI 默认 422。
- 未捕获的打分/模型错误：默认 500。
- 空候选：返回 success=true、空 items。
- 空模型：不会报错，仍以 `CF_V1` 返回规则基础分结果；这会掩盖“模型未加载”状态。
- reload 的 MySQL 失败会回退种子 SQL；种子文件再失败可能返回 500。
- reload API 没有服务自身鉴权，应只暴露在受控内网或增加服务间鉴权。

## 9. Item-CF 真实算法

### 9.1 行为权重

| 行为 | 实际基础权重 | 是否参与训练 |
|---|---:|---:|
| EXPOSE | 0 / 未配置 | 否 |
| CLICK | 1.0 | 是 |
| DETAIL | 1.5 + `min(1.0, stay_seconds/60)` | 是，单次最多 2.5 |
| LIKE | 2.0 | 是 |
| FAVORITE | 2.5 | 是 |
| COMMENT | 3.0 | 是 |

同一用户对同一 item 的多次行为会直接累加，没有按事件去重、时间衰减或单用户封顶。

### 9.2 矩阵和相似度

```text
user_items[userId]["TARGETTYPE_targetId"] += behaviorWeight
item_users[itemKey][userId] = accumulatedWeight
sim(i,j) = dot(userWeights_i, userWeights_j) / (norm_i * norm_j)
```

- 物品相似度是余弦相似度。
- 构建时两层遍历全部 item pair，没有 Top-K 邻居限制。
- item key 带 `CONTENT_`/`MATCH_`，但计算 pair 时不限制同类型，因此会学习 CONTENT-MATCH 跨类型相似度。
- 相似度只保存在内存字典，没有模型文件或数据库缓存。

### 9.3 候选 CF 分

```text
cfScore(candidate) = Σ(sim(candidate,hist) × accumulatedHistoryWeight) / Σ(sim(candidate,hist))
```

重要代码差异：文档和注释声称 CF 分数为 0～1，**实际并不成立**。分母只除以相似度之和，历史权重会累计并可大于 1，因此返回值没有 1 的上限；最终 `40 × cfScore` 也可能远超声明的 40 分上限并压过主队/直播等规则信号。

### 9.4 其他算法特征

- 未过滤用户已经点击、点赞或收藏过的候选。
- 仅对 Java 传来的近三天曝光 key 扣 100 分。
- 冷启动用户、未知 item、空模型均返回 CF=0，然后依靠 Python 内的规则基础分。
- 最终分：`rule_base + 40 × cfScore - exposedPenalty`。
- HOME_RECOMMEND/FOLLOWING_FEED 在 Python 侧做内容/比赛 7:3 穿插。
- 没有训练/在线评估版本号、模型数据窗口、模型 ID 随响应返回。

T18 必须先修正 CF 归一化/封顶、重复事件累加和 Top-K 问题，再把它作为可控加成接入。

## 10. Python 与数据库耦合

### 10.1 实际数据库访问

Python 只读数据库 `south_stand` 的 `user_behavior_log`：

```sql
SELECT user_id, behavior_type, target_type, target_id, stay_seconds, create_time
FROM user_behavior_log
WHERE is_deleted = 0
  AND status = 'ACTIVE'
  AND create_time >= ?
  AND user_id IS NOT NULL
```

没有读取 `content`、`content_relation`、`sys_user/user_account`、`follow_record`、`football_team`、`football_player` 或 `match_info`。没有 INSERT、UPDATE、DELETE、DDL。

### 10.2 是否强依赖 MySQL

- 运行层面：不强依赖；没设置 `DB_HOST` 时解析本地 seed SQL。
- 真实 T18 训练层面：需要持续行为数据，建议由 Python 用**专用只读账号**只读 `user_behavior_log`。
- 推荐请求层面：不应实时查询 MySQL，只使用内存模型和 Java 候选。

目标架构“Java 写日志、Python 只读日志训练、Java 提供候选、Python 返回分数”完全可行，且参考代码已实现大部分边界。

## 11. 当前本机 Python 环境

| 检查项 | 结果 |
|---|---|
| `py` launcher | 已安装 |
| 可发现 Python | 3.13.13，64-bit |
| 默认 `py` | Python 3.13.13 |
| `python` 命令 | 可运行，但命令定位显示 WindowsApps alias |
| `python3` | 未形成可依赖的独立命令 |
| `pip` 裸命令 | 不可用 |
| `py -m pip` | 可用，pip 26.0.1 |
| Conda | 未安装 |
| 同事 `.venv` | 存在但损坏；指向另一台机器不存在的 Python 3.12 路径 |
| 系统 FastAPI/Uvicorn/Pydantic/PyMySQL | 均未安装 |
| 是否可直接运行服务 | 否 |

后续建议（本轮未执行）：安装 Python 3.12.x 后，在正式 T18 Python 目录新建独立 venv，再从受控 requirements 安装依赖。不要修补或复用同事的失效 `.venv`。

## 12. MySQL / Redis 状态

| 组件 | 审计结果 |
|---|---|
| MySQL | Docker MySQL 8.0，127.0.0.1:3306 监听，可执行只读查询 |
| 正式数据库名 | `south_stand` |
| 数据库凭据 | 已配置，未输出 |
| Redis | Docker Redis 7，127.0.0.1:6379 监听，`PING -> PONG` |
| `user_behavior_log` | 当前正式库不存在 |
| 当前数据 | sys_user、content、match_info 均存在 |

T18 不需要新增 MySQL 实例，也不需要新增 Redis。推荐初版不需要 Kafka、Nacos、Elasticsearch或调度中心；当前项目代码也没有这些依赖。Redis 可继续服务现有登录失败缓存，不是 Item-CF 的前置条件。

## 13. 推荐服务端口

| 端口 | 当前状态 | 结论 |
|---:|---|---|
| 8080 | 未监听 | 当前后端默认端口，审计时未启动 |
| 8090 | 未监听 | 空闲 |
| 8100 | 未监听 | 空闲，建议继续作为 Python 推荐服务端口 |
| 8091 | 有其他进程监听，`/health` 返回 404 | 不建议占用 |

建议开发时仅绑定 `127.0.0.1:8100`；需要容器化或跨机访问时再调整监听地址和网络策略。参考实现的 `0.0.0.0` 会扩大暴露面。

## 14. Java 调 Python 的 HTTP Client 选择

当前项目：

- 已有 `spring-boot-starter-web`，Spring Boot 3.2.4 / Spring Framework 6.1 系列。
- 没有现成 RestTemplate、WebClient、OpenFeign、OkHttp、Apache HttpClient 或 JDK HttpClient 封装代码。
- 没有 WebFlux/OpenFeign/OkHttp 依赖。

建议使用 Spring `RestClient`：

- 已由 `spring-web` 提供，无需增加依赖。
- 同步调用模型与当前 MVC 技术栈一致。
- 容易集中配置 connect/read timeout、序列化、状态码和日志。
- 不建议直接复制同事手写的 `java.net.http.HttpClient + ObjectMapper + Map payload`，也不值得为单一内部服务引入 WebFlux 或 OpenFeign。

应在 `RecommendationRemoteClient` 外再加超时、空结果校验和轻量熔断/短期失败冷却；不要在 `FeedService` 里直接处理 HTTP。

## 15. 数据库 migration 冲突

### 15.1 当前正式主线

```text
V015__season_standings_ranks.sql
V016__team_roster_player_career.sql
V017__match_lineups_stats_ratings.sql
```

### 15.2 同事目录

```text
V015__season_standings_ranks.sql
V016__team_roster_player_career.sql
V017__recommend_behavior_tables.sql
V018__behavior_log_algorithm_version.sql
```

### 15.3 冲突判断

- 同事 `V017` 与正式 T17 的 V017 **直接冲突**，绝对不能执行或改名后原样复制。
- 同事 `V018` 只做 ALTER，前置依赖其冲突的 V017；当前库没有行为表，因此也不能单独执行。
- 同事后端快照缺少正式 T17 SQL/代码，进一步证明不能整体合并。
- 同事 V017/V018 的字段本身仅可作为设计参考。

### 15.4 T18 建议编号

建议重新编写：

```text
V018__recommendation_behavior_log.sql
```

该 migration 一次创建最终版行为表和索引，不依赖同事 V017/V018。演示行为使用独立的 `seed-t18-recommendation.sql`，不要把大量行为记录写入 migration。

## 16. `user_behavior_log` 审计

### 16.1 同事已有字段

| 字段 | 类型 | 情况 |
|---|---|---|
| id | BIGINT PK | 有 |
| user_id | BIGINT NULL | 有 |
| behavior_type | VARCHAR(32) | 有；相当于 event_type |
| target_type | VARCHAR(32) | 有 |
| target_id | BIGINT | 有 |
| scene | VARCHAR(32) NULL | 有 |
| algorithm_version | VARCHAR(32) NULL | V018 增加 |
| stay_seconds | INT NULL | 有，秒级 |
| extra_json | TEXT NULL | 有；position 被塞入 JSON |
| status/create_time/update_time/is_deleted | 通用字段 | 有 |

索引：

- `(user_id, create_time)`
- `(target_type, target_id, behavior_type)`
- `(scene, create_time)`
- `(algorithm_version, create_time)`

没有外键、业务唯一键或客户端事件幂等键。

### 16.2 缺失字段

- `experiment_id`
- `experiment_bucket`
- `request_id`
- `impression_id`
- 结构化 `position`
- `dwell_ms`（只有 `stay_seconds`）
- 客户端真实 `event_time`
- `client_event_id`（批量失败重试去重）
- 可选 `session_id` / anonymous id

### 16.3 当前能否回答归因问题

| 问题 | 能否可靠回答 | 原因 |
|---|---:|---|
| 点击来自哪一次 Feed 请求 | 否 | 没有 request_id/impression_id |
| 来自 RULE_V2 还是 CF_V1 | 部分 | EXPOSE/CLICK 可带 algorithmVersion；DETAIL 当前不带 |
| 用户当时在哪个实验桶 | 否 | 只能猜算法版本，没有 experiment_id/bucket |
| 卡片在第几个位置 | 部分 | 仅 EXPOSE 的 extra_json 可能有 position；CLICK 没有 |
| DETAIL 停留多久 | 是，粗粒度 | 有 stay_seconds，但无法关联具体曝光/点击 |

### 16.4 T18 建议结构

建议至少包含：

```text
id BIGINT PK
client_event_id VARCHAR(64) NOT NULL UNIQUE
user_id BIGINT NULL
session_id VARCHAR(64) NULL
behavior_type VARCHAR(32) NOT NULL
target_type VARCHAR(32) NOT NULL
target_id BIGINT NOT NULL
scene VARCHAR(32) NULL
algorithm_version VARCHAR(32) NULL
model_version VARCHAR(64) NULL
experiment_id VARCHAR(64) NULL
experiment_bucket VARCHAR(32) NULL
request_id VARCHAR(64) NULL
impression_id VARCHAR(64) NULL
position INT NULL
dwell_ms BIGINT NULL
event_time DATETIME(3) NOT NULL
extra_json JSON/TEXT NULL
status/create_time/update_time/is_deleted
```

建议索引：`(user_id,event_time)`、`(experiment_id,experiment_bucket,event_time)`、`(request_id,position)`、`(impression_id,behavior_type)`、`(target_type,target_id,behavior_type,event_time)`，并以 `client_event_id` 保证重试幂等。

## 17. 行为采集链路

| 行为 | 后端白名单/DTO支持 | Flutter 常量支持 | Flutter UI真实接入 | 结论 |
|---|---:|---:|---:|---|
| EXPOSE | 是 | 是 | 是 | 已接入 |
| CLICK | 是 | 是 | 是 | 已接入 |
| DETAIL | 是 | 是 | 是，仅内容详情 | 已接入但归因缺失 |
| LIKE | 是 | 是 | 否 | 代码支持但 UI 未接入 |
| FAVORITE | 是 | 是 | 否 | 代码支持但 UI 未接入 |
| COMMENT | 是 | 是 | 否 | 代码支持但 UI 未接入 |

后端一次最多接收 100 条；Flutter 内存累计 20 条自动 flush，失败保留，最多 500 条，未登录 401 被静默吞掉。后端逐条 insert，没有真正 batch insert；每条失败被捕获，缺表时更可能返回 `saved=0`，而非说明文档声称的整体 500。

## 18. EXPOSE 口径

当前实现是**分页数据下发/加载成功即曝光**，不是卡片真正进入可视区域：

- 首屏、刷新和加载更多收到页面后，FeedController 立即遍历所有卡片记录 EXPOSE。
- 没有 VisibilityDetector、viewport 回调或曝光时长阈值。
- refresh 会对同一批卡再次曝光；分页变化、重试或重复返回也可能重复。
- 没有 `impression_id`、`request_id`、`client_event_id`，服务端无法可靠去重。
- position 写入 `extraJson`，且是 0-based 全局位置；不是结构化字段。

T18 建议以“进入可视区域达到阈值（例如可见比例与持续时间）”为真实曝光，同时由一次 Feed 响应生成 requestId、每张卡生成 impressionId；客户端事件具备唯一 clientEventId。

## 19. DETAIL 归因问题

当前内容详情页从页面 state 创建时启动 Stopwatch，在 dispose 时上报：

```text
behaviorType=DETAIL
targetType=CONTENT
targetId
staySeconds
```

没有携带：`scene`、`algorithmVersion`、`experimentId`、`requestId`、`impressionId`、`position`、`dwellMs`。原因是跳转路由只传了内容标题，没有把信息流 attribution 上下文带入详情页。因此指标聚合会把 scene 和 algorithmVersion 归入 `UNKNOWN`，也无法证明 DETAIL 来自哪次推荐点击。

T18 应把 attribution 作为独立导航参数/上下文对象从 FeedCardRenderer 传到详情页，不能在详情页重新猜测当前 FeedController 状态。

## 20. A/B 分桶实现

同事实际代码：

```text
匿名用户 -> RULE_V2
登录用户 -> floorMod(userId * 31 + 7, 100) < remotePercent
remotePercent 默认 50
```

- 输入只有 userId，没有 experimentId。
- 同一用户分桶稳定，比例可配置为 0～100。
- 不支持多实验并行、实验版本切换、分桶盐轮换或明确 bucket 字段。
- 算法版本被当作实验桶替代品，实验定义与实现版本耦合。

T18 建议使用稳定哈希 `hash(experimentId + ':' + userId)`；响应和行为日志同时保存 experimentId、bucket、algorithmVersion。匿名用户如果要纳入实验，应使用稳定匿名/session ID；否则明确排除。

补充：模拟 seed 注释写“userId % 100”，但真实生成标签与 `(userId*31+7)%100` 一致；20 个用户中 11 个 CF、9 个 RULE，没有标签错桶。

## 21. RULE_V2 因子与当前数据能力

### 21.1 内容评分

| 因子 | 同事权重/公式 | 当前字段/Mapper | 批量实现能力 |
|---|---|---|---:|
| 主队关联 | +50 | user_profile.main_team_id + content_relation TEAM | 已有，可批量 |
| 关注球队 | +35 | follow_record + content_relation TEAM | 已有，可批量 |
| 关注球员 | +25 | follow_record + content_relation PLAYER | 已有，可批量 |
| 关注作者 | +45 | content.author_id + follow_record USER | 已有，可批量 |
| 内容热度 | `hot_score` | content.hot_score | 已有 |
| 点赞/评论/收藏 | 2/3/4 后先合并，再 log 缩放至约 30 上限 | content 三个计数字段 | 已有 |
| 发布时间 | `30*exp(-hours/24)` | content.publish_time | 已有 |
| 新内容保护 | 6 小时内零互动 +10 | publish_time + 互动计数 | 已有 |
| 近三天曝光 | -100 | 需要 user_behavior_log | 需新增，但应一次集合查询 |

### 21.2 比赛评分

| 因子 | 同事权重/公式 | 当前能力 |
|---|---|---:|
| LIVE | +80 | 已有 match_status |
| SCHEDULED | +40 + `20*exp(-hoursUntil/48)` | 已有 match_time |
| FINISHED 且有战报 | `25*exp(-hoursSinceEnd/48)` | 已有 has_report/match_report |
| 重要比赛 | 50 + importantLevel×5 | 已有 important_level |
| 主队比赛 | +50 | 已有主队与 home/away team |
| 关注球队比赛 | +35 | 已有 follow_record 与 home/away team |

### 21.3 实施分类

可以直接重新实现：上述所有规则因子，当前 T17 字段和 Mapper 基本齐全。  
需要新增数据：曝光日志、request/impression attribution、模型/实验版本。  
初版不建议：把所有重复规则再复制到 Python、实时查询核心业务表、逐候选查询关注/关系、复杂多模型融合。

只要候选特征继续来自现有 Batch Loader，RULE_V2 不需要引入 N+1。

## 22. MATCH 是否适合 CF

同事 Item-CF **技术上支持 MATCH**：行为 seed 含 MATCH，item key 包含 targetType，Python 会给 MATCH 计算相似度；但它还允许 CONTENT-MATCH 跨类型相似。

不建议 T18 初版把 MATCH 与 CONTENT 一起使用 CF：

- 比赛物品生命周期短、ID稀疏且历史复用价值低。
- 比赛状态、开赛时间、重要度、主队关系是更强、更可解释的信号。
- 参考数据只有 80 个比赛 item，且 MATCH 行为显著少于 CONTENT。
- 当前 CF 无时间衰减、Top-K 和类型约束，比赛结果容易受陈旧交互影响。

建议：

```text
CONTENT：RULE_V2 + 受控 CF_V1 加成/rerank
MATCH：RULE_V2
最终：Spring Boot 做去重、多样性和 7:3 混排
```

## 23. 降级链路

### 23.1 同事 Java 实现

RemoteRecommendClient 配置 connect timeout 和整请求 timeout，默认 800ms；以下情况返回 empty，FeedService 再调用 RULE_V2：

- 未启用远程服务。
- 连接失败或超时。
- HTTP 非 200，包括 422/500。
- 响应反序列化失败。
- `success=false`。
- items 为空。

RULE_V2 自身异常返回 failure；FeedService 如果没有得到可用推荐项，再按 hotScore + 互动量做 HOT fallback。因此设计链为：

```text
CF_V1 -> RULE_V2 -> HOT
```

Python 不启动时理论上首页仍可用。

### 23.2 缺口

- 空模型不会被识别为故障，Python 仍返回 `CF_V1`，实际却只有 Python 规则分。
- 没有熔断/失败冷却；CF 桶每次 Feed 都可能等待 800ms。
- 没有校验返回 item 是否覆盖/属于本次候选，只在 Java assemble 时忽略未知 key。
- Python 自身没有 recommend 异常包装和明确错误协议。
- 超时预算未区分 connect/read/overall，也没有指标。
- 规则权重在 Java/Python双写，易漂移。

T18 应保留三级降级，但增加模型 ready/version 校验、返回候选白名单校验、短期失败冷却和每层耗时/降级原因指标。

## 24. 模拟行为数据可迁移性

说明文档声称 6488 条；实际审计结果：

| 指标 | 实际值 |
|---|---:|
| SQL 文件总行数 | 6488 |
| 实际行为 tuple | **6416** |
| 用户数 | 20 |
| 唯一 CONTENT | 116 |
| 唯一 MATCH | 80 |
| 时间范围 | 2026-08-05 15:22:55 ～ 2026-08-20 14:53:43 |
| EXPOSE | 3623 |
| CLICK | 1187 |
| DETAIL | 1187 |
| LIKE | 213 |
| FAVORITE | 137 |
| COMMENT | 69 |
| CONTENT 行为 | 5183 |
| MATCH 行为 | 1233 |
| CF_V1 / RULE_V2 行为 | 3462 / 2954 |

ID 范围：

- user：`11000000000000001`～`11000000000000020`
- content：`16000000000000001`～`16000000000000116`
- match：`15000000000000001`～`15000000000000080`

只读查询当前 `south_stand` 后，这 20 个用户、116 个内容和80个比赛 ID 当前均存在；并且当前与同事的 `seed-demo.sql`、T15、T16 seed 内容一致。因此从“现时外键映射”看数据能对上。

仍不建议直接导入：

- migration 编号和表结构不兼容。
- 数据时间已经固定，过期后 MySQL 近30天查询会全部过滤掉。
- position 在 JSON 中，缺少请求/曝光/实验归因与幂等键。
- 数据是按旧埋点模型生成，不能验证 T18 新链路。
- 6416 记录与文档所称 6488 不一致，6488 实际是文件行数。

分类：ID 映射和概率生成思路可参考；SQL 需重新生成；原文件不能直接使用。T18 应基于开发库当时真实 user/content/match ID 和最终 V018 表结构生成 `seed-t18-recommendation.sql`。

## 25. 模型训练/重建方式

| 问题 | 当前实现 |
|---|---|
| 启动自动训练 | 是，FastAPI lifespan 中同步 rebuild |
| reload API | 有，POST，同步执行 |
| 定时训练 | 无 |
| 保存模型文件 | 无 |
| 每请求重算相似度 | 否，请求只读内存模型 |
| 模型切换 | rebuild 在局部变量完成后加锁替换字典 |
| MySQL窗口 | 默认近30天 |
| Seed窗口 | 不按 days 过滤，读取文件全部记录 |
| 算法复杂度 | item pair 两层循环，近似 O(I²)，无邻居截断 |

当前 116 CONTENT + 80 MATCH 的演示规模足够；若 item 上千，O(I²) 和全邻居字典会快速增长。T18 初版不必引入调度中心，可先采用：启动异步/预热重建 + 受保护 reload + 应用内简单定时任务（可后置）。正式联调前至少要避免启动线程被长训练阻塞，并暴露 modelReady、modelVersion、lastBuild、item/user/log count 和 build duration。

## 26. 当前后端建议新增模块

遵循当前项目按业务域组织的风格，建议使用现有 `com.southstand.recommend` 包，不另造顶层架构：

```text
com.southstand.recommend/
├── controller/
│   ├── BehaviorController
│   └── RecommendationMetricController
├── service/
│   ├── RecommendationService             # Feed 调用的统一门面
│   ├── RecommendationExperimentService   # 实验与稳定分桶
│   ├── BehaviorService
│   └── RecommendationMetricService
├── strategy/
│   ├── RecommendationStrategy
│   ├── RuleV2RecommendationStrategy
│   └── CfV1RecommendationStrategy
├── client/
│   └── RecommendationRemoteClient        # RestClient
├── mapper/
├── entity/
├── dto/
├── model/
└── vo/
```

此外建议将内容/比赛最终混排作为 Java 推荐域服务，而不是 Python 或 FeedController 的职责。

## 27. 当前后端需要修改的文件

预计最小改动面：

- `card/service/FeedService.java`：保留查询与批量加载，抽取候选特征并调用 RecommendationService；删除/迁移旧 score 责任；不要整体覆盖。
- `card/vo/FeedCardVO.java`：增加可选 `reason`，可能增加只供前端展示的 attribution 引用。
- `card/vo/FeedPageResult.java`：增加 algorithmVersion、experimentId、experimentBucket、requestId；每卡最好有 impressionId/position。
- `card/FeedServiceTests.java` 与 `FeedPerformanceStructureTests.java`：适配构造依赖并继续约束批量查询。
- `common/config`：集中创建带超时的 RestClient；安全规则明确行为接口和内部指标接口。
- `application*.yml`：后续增加推荐 endpoint、enabled、timeout、experiment 配置；本轮未修改。
- `scripts/sql/migrations/V018__recommendation_behavior_log.sql`：重新设计创建。
- 新增推荐域代码和测试，不改写 T17 业务域。

分页需要单独设计：如果下一页请求前已经写入曝光降权，重新排名会让卡片跨页移动并产生重复/遗漏。推荐响应应通过 requestId/snapshot/cursor 保持一次会话内排序稳定，不能照搬同事把 cursor 当页码的处理。

## 28. 明确禁止直接复制的同事代码

- 禁止整体复制或覆盖同事 `Tifo-main`。
- 禁止覆盖当前 `FeedService`，只能按当前 T17/T14 结构小步重构。
- 禁止执行或改名复制同事 `V017__recommend_behavior_tables.sql`。
- 禁止单独执行同事 V018 ALTER。
- 禁止覆盖当前 `schema.sql`、`seed-demo.sql` 或 incremental seeds。
- 禁止复制同事失效 `.venv`。
- 禁止原样复制 Python RULE_V2；Java/Python双写权重会漂移。
- 禁止原样采用同事行为表和下发即曝光口径。
- 禁止导入旧行为 seed 作为 T18 正式演示数据。

可以保留的设计思想：候选由 Java 掌握、Python 内存 CF、规则降级、稳定实验分桶、algorithmVersion 回传、批量行为上报、模型状态与手动 reload、内容/比赛最终混排。

## 29. 环境缺口清单

| 环境项 | 当前状态 | 是否必须 | 是否满足 | 后续动作 |
|---|---|---:|---:|---|
| JDK | 17.0.19 | 是 | 是 | 无 |
| Maven | 3.9.16 | 是 | 是 | 无 |
| MySQL | Docker MySQL 8.0，3306可访问 | 是 | 是 | 新增 V018 后再执行；本轮不执行 |
| Redis | Docker Redis 7，PONG | 项目必须/推荐非必须 | 是 | 无 |
| Python | 仅3.13.13 | 推荐服务必须 | 部分 | 建议安装3.12.x |
| pip | `py -m pip` 可用，裸 pip 不可用 | 是 | 是 | 统一使用 `python -m pip` |
| venv | 同事 venv 损坏 | 是 | 否 | 后续新建正式 venv |
| Python requirements | 系统未安装服务依赖 | 是 | 否 | 后续安装受控依赖 |
| Python推荐端口 | 8100空闲 | 是 | 是 | 建议绑定127.0.0.1:8100 |
| DB只读访问 | MySQL可访问，但推荐专用只读账号未核验 | 是 | 部分 | 后续创建/配置最小权限账号 |
| Java HTTP Client | Spring RestClient可直接使用 | 是 | 是 | 无需新增依赖 |
| Flutter | 3.44.6 / Dart 3.12.2 | 行为闭环需要 | 是 | 后续在正式前端基线验证 |
| Kafka/Nacos/ES | 推荐代码不依赖 | 否 | 不适用 | 不引入 |

### 29.1 阻塞 T18 联调

- 没有可运行的 Python 3.12 venv 和服务依赖。
- V018 最终行为表/归因字段尚未定稿。
- 推荐服务专用 MySQL 只读连接配置尚未建立。

### 29.2 不阻塞编码但建议先确定

- experimentId/bucket、requestId/impressionId/clientEventId 的端到端合同。
- CF 分数归一化、封顶、Top-K 和内容类型边界。
- 分页稳定性与曝光去重口径。
- Python endpoint 的鉴权/网络暴露方式。

### 29.3 后续优化

- 定时重建、模型持久化、熔断、监控、真实可视曝光、批量数据库 insert。

## 30. 当前阻塞问题与风险

按优先级：

1. **归因模型未定稿**：不先确定 request/impression/experiment/client event，后补会导致表、API、Flutter 全链路返工。
2. **Python运行环境不可用**：解释器存在但服务依赖缺失；同事 venv 不能迁移。
3. **migration冲突**：同事 V017 已被正式 T17 占用。
4. **CF数值与文档不一致**：实际 cfScore 不限 0～1，可能让 CF 加成失控。
5. **职责重复**：RULE_V2 和混排同时存在于 Python/Java，未来必然漂移。
6. **曝光和分页不稳定**：下发即曝光、无幂等、曝光降权会改变后续页排序。
7. **空模型伪装成CF**：仍返回 CF_V1，A/B 指标会污染。
8. **同事后端缺 T17**：直接复制会丢失正式主线能力。

## 31. T18 推荐实施顺序

1. 冻结 T17 基线，明确正式前端仓库/分支和接口版本。
2. 定稿 recommendation attribution 合同：experiment、request、impression、position、client event、event time、dwell ms。
3. 编写并评审新的 `V018__recommendation_behavior_log.sql`，不要执行同事 SQL。
4. 在当前后端新增行为 DTO/Service/Mapper/Controller 和幂等批量写入测试。
5. 保留 T14 Batch Loader，抽取轻量 Feed candidate 模型与 RecommendationService 门面。
6. 在 Java 重新实现单一来源 RULE_V2，先保证 RULE_V2 -> HOT 降级。
7. 安装 Python 3.12、创建新 venv、锁定依赖，先运行 health/selftest。
8. 把 Python 收敛为 CONTENT Item-CF score/rerank；修正归一化、封顶、Top-K、已交互过滤/衰减策略。
9. 使用 Spring RestClient 接入 CF，完成 CF -> RULE -> HOT、模型未就绪、超时、500、空/非法结果测试。
10. Java 完成 CONTENT/MATCH 去重、多样性与 7:3 混排，设计稳定 cursor/request snapshot。
11. Flutter 接入真实可视 EXPOSE 和完整 attribution；再接 LIKE/FAVORITE/COMMENT。
12. 基于当前数据库真实 ID 重新生成 T18 incremental behavior seed。
13. 完成指标、A/B 验证、性能与故障演练后再逐步提高 CF 流量。

## 最终结论

```text
当前是否具备 T18 开发条件：部分具备

当前可直接使用的环境：
- JDK 17.0.19
- Maven 3.9.16
- MySQL 8.0 / south_stand
- Redis 7
- Flutter 3.44.6 / Dart 3.12.2
- Spring Boot 3.2.4 自带的 Spring RestClient
- 空闲端口 8100

需要新增的环境：
- 独立、可复现的 Python 3.12 venv
- 推荐服务专用 MySQL 只读连接身份

需要安装的软件：
- 建议安装 Python 3.12.x
- 在新 venv 中安装并锁定 FastAPI、Uvicorn、Pydantic v2、PyMySQL

需要新增的配置：
- 推荐服务 enabled / endpoint / timeout
- experimentId / bucket / trafficPercent / salt
- Python MySQL 只读连接配置
- modelReady / modelVersion 与 reload 保护配置

Python 推荐服务建议版本：
- Python 3.12.x（参考环境为 3.12.14）

Python 建议端口：
- 127.0.0.1:8100

Python 是否需要直连 MySQL：
- T18 初版建议需要，但只能只读 user_behavior_log；推荐请求不能实时查业务表

当前推荐算法是否可以接入：
- 可以参考并改造接入；候选输入模式已经正确，但必须修正 CF 归一化、职责边界和归因字段

是否建议直接复制同事代码：
- 否

建议保留的同事设计：
- Java掌握候选集
- Python内存Item-CF
- CF -> RULE -> HOT三级降级
- 稳定A/B分桶思想
- 行为批量上报、模型状态和reload
- 推荐理由与算法版本回传

需要重新实现的部分：
- 当前T17上的Recommendation模块
- RULE_V2单一来源实现
- V018行为表及完整归因合同
- RestClient远程调用和降级/熔断
- Java最终混排与稳定分页
- Flutter真实曝光和完整行为闭环
- 基于当前ID的新T18行为seed

当前最大技术风险：
- 缺少request/impression/experiment级归因，加上CF分数实际未归一化，会同时污染排序和A/B结论

当前阻塞问题：
- Python 3.12运行环境/依赖未就绪
- V018及端到端归因字段尚未定稿
- 推荐专用数据库只读配置未建立

T18 是否可以正式开工：
- 可以开始架构与合同评审；完成上述三项后再进入正式编码和联调
```

