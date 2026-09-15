# Backend V1 前端开发交接文档

> 项目：南看台 / Tifo
> 整理日期：2026-08-31
> 契约基线：Backend V1 Freeze（2026-08-30）
> 快速启动：[FRONTEND_BACKEND_QUICKSTART_V1.md](./FRONTEND_BACKEND_QUICKSTART_V1.md)

## 1. Backend V1 状态

T00-T22 已完成，Backend V1 已冻结。43 页第一版需求的后端能力已覆盖，明确延期或选做能力除外。当前足球数据主要是人造 Demo 数据，Flutter 可以直接按冻结 API 开发和联调。

冻结规则：

- 不修改已有 path、字段含义、枚举值和分页结构。
- 后续原则上只允许兼容新增字段，除非确认 V1 存在 Bug。
- Flutter 必需字段缺失时，优先兼容新增，不破坏旧客户端。
- 前端不要依赖未写入冻结契约的内部实现细节。

T22 验收记录为 Java 145/145、Python 8/8，完整 Smoke 已通过。本轮只整理文档并进行轻量启动检查，不重复全量验收。

## 2. 整体架构

```text
Flutter
  |
  v
Spring Boot :8080
  |-- MySQL :3306
  |-- Redis :6379
  `-- Recommendation Client
          |
          v
      Python CF Service :8100
```

- Flutter 只访问 Spring Boot，不直接访问 MySQL、Redis 或 Python。
- Python 不可达或模型未准备好时，Java 从 CF 推荐自动降级到 `RULE_V2`。
- Python 故障不应导致 Flutter 首页或 Feed 不可用。
- Python 内部接口不是移动端契约。

## 3. 环境要求

| 组件 | 真实项目要求/本机已验证 |
| --- | --- |
| JDK | 17 / Temurin 17.0.19 |
| Spring Boot | 3.2.4 |
| Maven | 3.9.16 |
| MySQL | 8.x / 当前容器 `mysql:8.0` |
| Redis | 7.x / 当前容器 `redis:7` |
| Python | 3.13.13 |

Python 依赖声明在 `recommend-service/requirements.txt`，包括 FastAPI、Uvicorn、Pydantic 2、PyMySQL、pytest 和 httpx。Spring Boot 默认激活 `dev` Profile。

## 4. Windows 本地完整启动

启动顺序：MySQL、Redis、Python 推荐服务、Spring Boot。

### 4.1 MySQL 与 Redis

开发配置默认目标为 MySQL `127.0.0.1:3306/south_stand`，Redis `127.0.0.1:6379`、DB `0`。

```powershell
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}"
Test-NetConnection 127.0.0.1 -Port 3306
Test-NetConnection 127.0.0.1 -Port 6379
```

数据库密码、JWT Secret 等只通过本地环境变量提供。文档和 Flutter 工程中不得保存真实值。

### 4.2 Python 推荐服务

```powershell
.\scripts\windows\start-recommend-service.ps1
Invoke-RestMethod http://127.0.0.1:8100/health | ConvertTo-Json
```

脚本实际启动 `py -m uvicorn app.main:app --host 127.0.0.1 --port 8100`，使用隐藏窗口并记录 PID。`modelReady=true` 表示 CF 模型可用；false 或 8100 不可达时 Java 使用规则降级。

停止命令：`.\scripts\windows\stop-recommend-service.ps1`。

### 4.3 Spring Boot

```powershell
.\scripts\windows\run-dev.ps1
```

脚本执行 `mvn spring-boot:run`，默认 Profile 为 `dev`，端口为 `8080`。Swagger 为 `http://127.0.0.1:8080/doc.html`。

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/public/health
Invoke-RestMethod http://127.0.0.1:8080/api/public/health/db
Invoke-RestMethod http://127.0.0.1:8080/api/public/health/redis
```

Android 模拟器使用 `http://10.0.2.2:8080`，或先执行 `adb reverse tcp:8080 tcp:8080`。真机使用开发机局域网 IP。

## 5. 5 分钟启动 Backend V1

```powershell
Test-NetConnection 127.0.0.1 -Port 3306
Test-NetConnection 127.0.0.1 -Port 6379
.\scripts\windows\start-recommend-service.ps1
Invoke-RestMethod http://127.0.0.1:8100/health
# 新终端
.\scripts\windows\run-dev.ps1
# Java 启动完成后
Invoke-RestMethod http://127.0.0.1:8080/api/public/health
Invoke-RestMethod 'http://127.0.0.1:8080/api/app/feed?tab=recommend&pageNum=1&pageSize=10'
```

## 6. 如何停止服务

- Python：`.\scripts\windows\stop-recommend-service.ps1`。
- Spring Boot：在运行终端按 `Ctrl+C`。
- 检查残留：`Get-NetTCPConnection -LocalPort 8080,8100 -State Listen -ErrorAction SilentlyContinue`。

结束 PID 前必须用 `Get-CimInstance Win32_Process` 核对命令行，确认属于本项目。

## 7. 数据库与 Demo 数据

数据库名为 `south_stand`。`scripts/sql` 下的 schema、migration、seed 分别用于建表、增量演进和演示数据初始化。前端不连接数据库，也不根据表结构推测 API。

当前足球数据为合成 Demo 数据。T22 冻结基线记录约有：11 联赛、22 赛季、30 球队、262 球员、67 场 FINISHED、12 场 SCHEDULED、7 场 LIVE、247 条内容、605 条评论、42 条通知、6342 条推荐行为；另含 55 条积分榜、466 条球员赛事统计、55 条球队赛事统计、301 条评分等。数据可能因本地只读联调发生变化。

ID 不保证清库后稳定；页面应从列表、搜索或关联对象获得 ID；不要依赖固定顺序；部分实体数据可能为空；不要把 Demo 数量、名称或内容当作官方真实数据；使用接口返回的 availability/status 字段。

## 8. 统一响应、分页与错误码

### 8.1 Result

当前代码的真实顶层结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "..."
}
```

实际实现没有顶层 `timestamp`。成功条件是 `code == 0`，不要只依赖 HTTP 200。错误日志保留 `traceId`。

### 8.2 PageResult

```json
{
  "records": [],
  "total": 0,
  "pageNum": 1,
  "pageSize": 20,
  "pages": 0
}
```

ID 为 Java `Long`，Dart 使用 `int`，不要按 32 位处理。时间为 ISO 本地日期时间字符串；解析时允许 null。列表可能为 `[]`，单对象可能为 null，两者不能混用。

### 8.3 错误码

| code | 含义 | 前端建议 |
| --- | --- | --- |
| `0` | 成功 | 读取 data |
| `40001` | 参数错误 | 展示 message/表单提示 |
| `40101` | 未登录 | 清 Token，进入登录 |
| `40102` | Token 失效或过期 | 清 Token，进入登录 |
| `40103` | 登录凭据错误 | 留在登录页提示 |
| `40301` | 无权限 | 禁止页或轻提示 |
| `40401` | 资源不存在 | 空态/已删除态 |
| `40901` | 状态冲突 | 提示并刷新 |
| `40902` | 已废弃兼容码 | 不新增依赖 |
| `50001` | 服务内部错误 | 通用错误，可带 traceId 上报 |

## 9. 鉴权与 Security 边界

注册 `POST /api/auth/register`、登录 `POST /api/auth/login` 公开；`GET /api/auth/me` 需要 JWT。注册字段：username（3-64 位字母、数字或下划线）、可选 phone（不超过 32）、password（6-64）。登录响应包含 accessToken、tokenType、expiresIn、user。

```http
Authorization: Bearer <accessToken>
```

默认过期 604800 秒。V1 没有 refresh-token 和 logout API；客户端注销就是删除本地 Token。

匿名可访问 Feed、内容/评论读取、搜索、公开用户 profile/contents/followings/followers、大部分足球 GET、`/api/public/**`、文件读取、Health 和 Swagger。`GET /api/app/football/matches/following-teams` 例外，需要登录。

发布、评论写操作、点赞、收藏、关注、上传、个人中心、通知、推荐行为上报，以及未列为公开的 `/api/app/**` 均需登录。`/api/admin/**` 和 `/api/internal/**` 需要 ADMIN，移动端不应调用。

## 10. Onboarding 与关注

- `GET /api/app/onboarding/options`
- `POST /api/app/onboarding/preferences`

提交必填 `mainTeamId`，可带 `followTeamIds`、`followPlayerIds`；主队会自动关注。

当前真实代码没有“最多关注 5 支球队”的通用上限，也没有用户关注数量上限。前端不要自行加入 5 支硬限制；推荐少选只能作为界面引导。

关注切换为 `POST /api/app/follows/toggle`，目标类型 `USER`、`TEAM`、`PLAYER`。

## 11. Feed 与推荐卡片

Feed：`GET /api/app/feed`。参数为 tab（默认 recommend）、leagueId、teamId、pageNum（默认 1）、pageSize（默认 10）、cursor。热门联赛：`GET /api/app/feed/hot-leagues?limit=10`。

冻结 tab：`recommend`、`news`、`following`、`team`。历史说明中的 `mixed`、`match` 不应作为 V1 依赖。

FeedPage 除分页外还可能包含 nextCursor、algorithmVersion、modelVersion、experimentId、experimentBucket、requestId。

| cardType | 建议组件 | 关键数据 |
| --- | --- | --- |
| `CONTENT` | 内容卡 | 内容、作者、发布时间、互动数、媒体、关联标签 |
| `MATCH` | 比赛卡 | 双方球队、时间、状态、比分 |
| `HOT_COMMENT` | 热评卡 | commentId、contentId、文本、作者、互动数、hotScore |
| `DISCUSSION` | 讨论卡 | contentId、title、summary、author、互动数、hotComment |
| `RANKING` | 排名卡 | rankingType、rankType、league/season、title、items |
| `PLAYER_RATING` | 球员评分卡 | match/league、比赛信息、topPlayers、ratingUserCount |

公共字段包括 cardId、cardKey、cardType、algorithmVersion、payload，以及可能平铺的业务字段、reasonCode、reason、impressionId、position。

Flutter 必须以 cardType 建立判别联合类型，保留推荐归因字段，为未知 cardType 提供安全占位或跳过。早期文档的 `CONTENT_CARD/MATCH_CARD` 已不是现行值，当前是 `CONTENT/MATCH`。

当前代码可返回的算法版本包括 `RULE_V2`、`CF_V1`、辅助卡片使用的 `AUX_RULE_V1`，以及热门流使用的 `HOT_V1`。页面级 attribution 包含 algorithmVersion、modelVersion、experimentId、experimentBucket、requestId；卡片级包含 impressionId、position、reasonCode、reason。

## 12. 搜索

`GET /api/app/search/entities`，必填 keyword，可选 entityType，pageNum 默认 1，pageSize 默认 20。实体类型：`TEAM`、`PLAYER`、`MATCH`、`CONTENT`。

不传 entityType 时会聚合 TEAM、PLAYER、MATCH、CONTENT，再统一排序分页。TEAM 匹配中英文队名和简称；PLAYER 匹配中英文球员名；MATCH 匹配主客队或联赛名称，纯数字 keyword 还可匹配比赛 ID；CONTENT 真实匹配 title、summary、body。

比赛结果还包含主客队、matchStatus、matchTime；内容结果包含 contentType、publishTime。按 entityType 建模和跳转，不要假定所有结果字段相同。

## 13. 内容、评论与互动

### 13.1 内容

- `GET /api/app/contents/{id}`
- `POST /api/app/contents/posts`
- `POST /api/app/contents/articles`
- `PUT /api/app/contents/{id}/articles`

帖子 title 必填且不超过 255，body 不超过 2000，mediaUrls/fileIds 最多 9 个，relations 最多 10 个。文章包含 title、summary、coverFileId、blocks、relationList；block 使用 blockType、text、mediaFileId、sortOrder。

### 13.2 评论

- `GET /api/app/comments`：targetType/targetId 或兼容 contentId，sort 默认 hot，可带 parentId/page/pageSize。
- `GET /api/app/comments/{commentId}/replies`
- `GET /api/app/comments/hot?contentId={id}&limit={n}`
- `POST /api/app/comments`
- `POST /api/app/comments/{id}/likes/toggle`
- `DELETE /api/app/comments/{id}`

创建评论使用 targetType、targetId（或 contentId）、parentId、replyToUserId、`contentText`。`content` 是历史兼容字段，新客户端优先 contentText。

### 13.3 点赞、收藏、关注

- `POST /api/app/likes/toggle`：CONTENT、COMMENT。
- `POST /api/app/favorites/toggle`：当前只支持 CONTENT。
- `POST /api/app/follows/toggle`：USER、TEAM、PLAYER。

Toggle 响应是最终状态来源；乐观更新失败时回滚。

## 14. 文件上传与媒体

- `POST /api/app/files/upload`：JWT，multipart 字段 file 和可选 bizType。
- bizType：AVATAR、CONTENT_IMAGE、COMMENT_IMAGE、GENERAL_IMAGE。
- jpg/jpeg/png/webp/gif，单文件最大 10MB。
- `GET /api/public/files/{fileId}`：公开读取。
- `DELETE /api/app/files/{id}`：删除可管理文件。
- `POST /api/app/users/me/avatar`：body 使用 fileId。

相对媒体 URL 使用 Spring Boot base URL 拼接，不能拼 Python 地址。

## 15. 足球数据与页面 API 映射

### 15.1 基础、比赛和排名

- `GET /api/app/football/leagues`
- `GET /api/app/football/leagues/{leagueId}/seasons`
- `GET /api/app/football/leagues/{leagueId}/seasons/{seasonId}/stages`
- `GET /api/app/football/matches/important?date=...&page=...`
- `GET /api/app/football/matches/following-teams`：JWT。
- `GET /api/app/football/matches?leagueId=&teamId=&date=&status=&page=`
- `GET /api/app/football/matches/{id}`
- `GET /api/app/football/matches/{id}/overview`
- `GET /api/app/football/matches/{id}/lineups`
- `GET /api/app/football/matches/{id}/stats`
- `GET /api/app/football/matches/{id}/player-stats`
- `GET /api/app/football/matches/{id}/ratings`

比赛状态为 SCHEDULED、LIVE、FINISHED。球员评分 1-10，步长 0.5。Overview 聚合 match、lineups、teamstats、playerstats、ratings、ranking，适合首屏；子接口用于 Tab 独立刷新。

Ranking 的 snapshotType 当前是 `CURRENT_STANDING` 或 `UNAVAILABLE`，不是赛前历史快照。UI 必须显示“当前排名”，不能显示成“赛前排名”。

积分榜按 leagueId、seasonId 查询，可带 stage/group。球员榜类型：GOALS、ASSISTS、YELLOW_CARDS、RED_CARDS、SHOTS、SHOTS_ON_TARGET、RATING、SAVES、APPEARANCES、MINUTES。球队榜类型：GOALS_FOR、GOALS_AGAINST、ASSISTS、YELLOW_CARDS、RED_CARDS、SHOTS、SHOTS_ON_TARGET、CORNERS、FOULS、CLEAN_SHEETS、AVG_RATING。

### 15.2 Team Detail

| Tab | 对应能力 |
| --- | --- |
| 基础信息 | `GET /api/app/football/teams/{teamId}` |
| 概览 | `GET /api/app/football/teams/{teamId}/overview?seasonId=` |
| 阵容 | `GET /api/app/football/teams/{teamId}/players?seasonId=&position=&squadRole=&pageNum=1&pageSize=50` |
| 数据 | `GET /api/app/football/teams/{teamId}/stats?seasonId=&stageId=` |
| 荣誉 | `GET /api/app/football/teams/{teamId}/honors?honorType=` |
| 赛程 | `GET /api/app/football/teams/{teamId}/matches?status=&pageNum=1&pageSize=20` |
| 动态 | `GET /api/app/football/teams/{teamId}/contents?contentType=&pageNum=1&pageSize=20` |

路径位于 `/api/app/football/teams/{teamId}/...`。具体后缀以 Swagger 和冻结 Controller 为准，不自行组合不存在的路径。

### 15.3 Player Detail

| Tab | 对应能力 |
| --- | --- |
| 基础信息 | `GET /api/app/football/players/{playerId}` |
| 概览 | `GET /api/app/football/players/{playerId}/overview?seasonId=` |
| 数据 | `GET /api/app/football/players/{playerId}/stats?seasonId=&leagueId=&stageId=` |
| 效力球队 | `GET /api/app/football/players/{playerId}/teams` |
| 生涯 | `GET /api/app/football/players/{playerId}/career` |
| 比赛 | `GET /api/app/football/players/{playerId}/matches?pageNum=1&pageSize=20` |
| 动态 | `GET /api/app/football/players/{playerId}/contents?contentType=&pageNum=1&pageSize=20` |

PlayerOverview 包含 club 与 nationalTeam；国家队字段可能为 null，Flutter Model 和页面都必须兼容。

## 16. 用户中心、公开主页与通知

`/api/app/users/me` 下提供 profile GET/PUT、summary、stand、contents、favorites、likes、comments、avatar，全部需要 JWT。

公开主页提供 profile、follow/unfollow、followings、followers、contents、favorites、comments。用户关系枚举为 `SELF`、`NONE`、`FOLLOWING`、`FOLLOWED_BY`、`MUTUAL`。Security 实际只允许匿名读取 profile、contents、followings、followers；favorites/comments 需要登录，并可能因隐私规则返回 40301。不要把所有 Tab 都当作匿名公开。

通知全部需要 JWT：`GET /api/app/notifications` 支持 pageNum/pageSize/type/readStatus；`GET /api/app/notifications/unread-count`；`POST /api/app/notifications/{notificationId}/read`；`POST /api/app/notifications/read-all`。通知类型为 `CONTENT_LIKED`、`CONTENT_COMMENTED`、`COMMENT_REPLIED`、`COMMENT_LIKED`、`USER_FOLLOWED`、`SYSTEM`；目标类型为 `CONTENT`、`COMMENT`、`USER`、`SYSTEM`。`targetAvailable=false` 时禁用跳转并显示目标不可用。

## 17. 推荐归因与行为上报

Flutter 只向 Java 上报 `POST /api/app/recommendation/behaviors/batch`，JWT，单批最多 100 条。

事件字段：clientEventId、sessionId、behaviorType、targetType、targetId、scene、algorithmVersion、modelVersion、experimentId、experimentBucket、requestId、impressionId、position、dwellMs、eventTime、extraJson。

行为类型：EXPOSE、CLICK、DETAIL、LIKE、FAVORITE、COMMENT。目标类型：CONTENT、MATCH、COMMENT、RANKING、PLAYER_RATING。

- EXPOSE：卡片实际进入可见区域后，不是在接口返回时。
- CLICK：用户点击卡片时。
- DETAIL：确实进入详情后，保留来源卡片归因，可补 dwellMs。
- LIKE/FAVORITE/COMMENT：业务接口成功后上报。

响应包含 received、saved、duplicated、rejected。clientEventId 应稳定唯一以便重试去重。失败事件可短暂排队，但不能阻塞业务。`/api/internal/recommendation/metrics` 需要 ADMIN，不属于 Flutter。

## 18. API 总览（按 Flutter 场景）

下表中的“登录”指携带 Bearer JWT。足球只读接口除 following-teams 外均可匿名。

| 场景 | Method | Path | 登录 | 主要参数 | Flutter 页面/动作 |
| --- | --- | --- | --- | --- | --- |
| Authentication | POST | `/api/auth/register` | 否 | username、phone?、password | 注册 |
| Authentication | POST | `/api/auth/login` | 否 | username、password | 登录 |
| Authentication | GET | `/api/auth/me` | 是 | 无 | 恢复会话 |
| Onboarding | GET | `/api/app/onboarding/options` | 是 | 无 | 主队/关注选择 |
| Onboarding | POST | `/api/app/onboarding/preferences` | 是 | mainTeamId、followTeamIds、followPlayerIds | 完成引导 |
| Feed | GET | `/api/app/feed` | 否 | tab、leagueId?、teamId?、pageNum、pageSize、cursor? | 首页 |
| Feed | GET | `/api/app/feed/hot-leagues` | 否 | limit | 首页联赛筛选 |
| Search | GET | `/api/app/search/entities` | 否 | keyword、entityType?、pageNum、pageSize | 全局搜索 |
| Content | GET | `/api/app/contents/{contentId}` | 否 | contentId | 内容详情 |
| Content | POST | `/api/app/contents/posts` | 是 | CreatePostRequest | 发布帖子 |
| Content | POST | `/api/app/contents/articles` | 是 | ArticleRequest | 发布文章 |
| Content | PUT | `/api/app/contents/{contentId}/articles` | 是 | ArticleRequest | 编辑文章 |
| Comment | GET | `/api/app/comments` | 否 | targetType/targetId 或 contentId、sort、parentId?、分页 | 评论列表 |
| Comment | GET | `/api/app/comments/{commentId}/replies` | 否 | sort、分页 | 回复列表 |
| Comment | POST | `/api/app/comments` | 是 | target、parent/reply、contentText | 发表评论 |
| Interaction | POST | `/api/app/likes/toggle` | 是 | targetType、targetId | 点赞 |
| Interaction | POST | `/api/app/favorites/toggle` | 是 | CONTENT、targetId | 收藏 |
| Follow | POST | `/api/app/follows/toggle` | 是 | USER/TEAM/PLAYER、targetId | 关注切换 |
| League | GET | `/api/app/football/leagues` | 否 | 无 | 联赛选择 |
| Season | GET | `/api/app/football/leagues/{leagueId}/seasons` | 否 | leagueId | 赛季选择 |
| Stage | GET | `/api/app/football/leagues/{leagueId}/seasons/{seasonId}/stages` | 否 | leagueId、seasonId | 阶段选择 |
| Match | GET | `/api/app/football/matches` | 否 | leagueId?、teamId?、date?、status?、分页 | 比赛列表 |
| Match | GET | `/api/app/football/matches/{matchId}/overview` | 否 | matchId | 比赛详情首屏 |
| Match | GET | `/api/app/football/matches/{matchId}/lineups` | 否 | matchId | 阵容 Tab |
| Match | GET | `/api/app/football/matches/{matchId}/stats` | 否 | matchId | 统计 Tab |
| Match | GET | `/api/app/football/matches/{matchId}/ratings` | 否 | teamId? | 评分 Tab |
| Match | POST/DELETE | `/api/app/football/matches/{matchId}/players/{playerId}/ratings` | 是 | POST body rating | 用户评分/撤销 |
| Ranking | GET | `/api/app/football/standings` | 否 | leagueId、seasonId、stageId?、groupCode? | 积分榜 |
| Ranking | GET | `/api/app/football/player-ranks` | 否 | leagueId、seasonId、stageId?、rankType、分页 | 球员榜 |
| Ranking | GET | `/api/app/football/team-ranks` | 否 | leagueId、seasonId、stageId?、rankType、分页 | 球队榜 |
| User Center | GET | `/api/app/users/me/summary` | 是 | 无 | 我的概览 |
| User Center | GET | `/api/app/users/me/{contents,likes,favorites,comments}` | 是 | 各接口分页参数 | 我的各列表 |
| Social | GET | `/api/app/users/{userId}/profile` | 否 | userId | 其他用户主页 |
| Social | GET | `/api/app/users/{userId}/{followings,followers}` | 否 | 分页 | 关注/粉丝 |
| Social | POST/DELETE | `/api/app/users/{userId}/follow` | 是 | userId | 关注/取消关注用户 |
| Notification | GET | `/api/app/notifications` | 是 | pageNum、pageSize、type?、readStatus? | 通知中心 |
| Notification | GET | `/api/app/notifications/unread-count` | 是 | 无 | 红点 |
| Recommendation | POST | `/api/app/recommendation/behaviors/batch` | 是 | events，最多 100 | 埋点批量上报 |
| File | POST | `/api/app/files/upload` | 是 | multipart file、bizType? | 图片上传 |
| File | GET | `/api/public/files/{fileId}` | 否 | fileId | 图片展示 |

Admin/Internal 仅供后台或运维：`GET /api/admin/health`、`GET /api/admin/dashboard/summary`、`GET/PUT /api/admin/users...`、`GET/PUT /api/admin/contents...`、`GET /api/internal/recommendation/metrics`。普通 Flutter App 不调用。

### Match Detail Tab 映射

| Tab | API |
| --- | --- |
| 总览 | `GET /api/app/football/matches/{matchId}/overview` |
| 阵容 | `GET /api/app/football/matches/{matchId}/lineups` |
| 排名 | overview.ranking，语义为 CURRENT_STANDING |
| 统计 | `GET /api/app/football/matches/{matchId}/stats` 和 player-stats |
| 评分 | `GET /api/app/football/matches/{matchId}/ratings`；提交/撤销使用 player ratings POST/DELETE |

## 19. 高频请求与响应示例

以下仅展示前端建模所需的真实字段子集；未展示字段仍以实际响应为准。

### 19.1 登录

```http
POST /api/auth/login
Content-Type: application/json

{"username":"demo_user","password":"<local-password>"}
```

```json
{"code":0,"message":"success","data":{"accessToken":"<jwt>","tokenType":"Bearer","expiresIn":604800,"user":{"id":1,"username":"demo_user"}},"traceId":"..."}
```

### 19.2 Onboarding

```json
{"mainTeamId":101,"followTeamIds":[101,102],"followPlayerIds":[1001,1002]}
```

响应 data 字段为 completed、mainTeamId、followTeamCount、followPlayerCount。数组没有 5 支硬上限。

### 19.3 Feed 与搜索

```http
GET /api/app/feed?tab=recommend&pageNum=1&pageSize=10
GET /api/app/search/entities?keyword=arsenal&entityType=TEAM&pageNum=1&pageSize=20
```

Feed data 包含 records 和 nextCursor/algorithmVersion/modelVersion/experimentId/experimentBucket/requestId；每条卡片包含 cardId、cardKey、cardType、payload、impressionId、position 等。搜索 data 使用标准 PageResult。

### 19.4 内容与评论

```http
GET /api/app/contents/123
GET /api/app/comments?contentId=123&sort=hot&pageNum=1&pageSize=10
```

评论创建示例：

```json
{"targetType":"CONTENT","targetId":123,"parentId":null,"replyToUserId":null,"contentText":"这是一条评论"}
```

### 19.5 球队、球员、比赛与排名

```http
GET /api/app/football/teams/101/overview?seasonId=2025
GET /api/app/football/teams/101/matches?status=FINISHED&pageNum=1&pageSize=20
GET /api/app/football/players/1001/overview?seasonId=2025
GET /api/app/football/players/1001/matches?pageNum=1&pageSize=20
GET /api/app/football/matches/5001/overview
GET /api/app/football/matches/5001/lineups
GET /api/app/football/matches/5001/stats
GET /api/app/football/standings?leagueId=1&seasonId=2025
GET /api/app/football/player-ranks?leagueId=1&seasonId=2025&rankType=GOALS&pageNum=1&pageSize=20
GET /api/app/football/team-ranks?leagueId=1&seasonId=2025&rankType=GOALS_FOR&pageNum=1&pageSize=20
```

TeamOverview 的常用字段包括 teamId、teamName、seasonId、standing、seasonStats、topScorers、recentMatches、nextMatch、recentContents、honors。PlayerOverview 包括 playerId、playerName、position、nationality、currentTeam、seasonStats、career、club、nationalTeam、recentMatches；nationalTeam 可为 null。MatchOverview 包含 match、lineups、teamStats、playerStats、ratings、ranking。

### 19.6 通知

```http
GET /api/app/notifications?pageNum=1&pageSize=10&readStatus=UNREAD
GET /api/app/notifications/unread-count
POST /api/app/notifications/{notificationId}/read
POST /api/app/notifications/read-all
```

列表记录应读取 notificationType、targetType、targetId、read、readTime、createTime 和 targetAvailable；actor、secondaryTarget 与 targetPreview 均按 nullable 处理。readStatus 是列表查询参数，支持 UNREAD/READ（也兼容 0/1）。

### 19.7 推荐行为批量上报

```json
{
  "events": [{
    "clientEventId": "uuid-or-stable-id",
    "sessionId": "session-id",
    "behaviorType": "DETAIL",
    "targetType": "CONTENT",
    "targetId": 123,
    "scene": "HOME_RECOMMEND",
    "algorithmVersion": "CF_V1",
    "modelVersion": null,
    "experimentId": null,
    "experimentBucket": null,
    "requestId": "feed-request-id",
    "impressionId": "impression-id",
    "position": 1,
    "dwellMs": 3200,
    "eventTime": "2026-08-31T20:00:00",
    "extraJson": null
  }]
}
```

## 20. Flutter 数据模型建议

- Java Long ID 映射 Dart `int`，不做 32 位截断。
- 所有后端 nullable 字段在 Dart 中保持 nullable；尤其国家队、媒体、评分、ranking 和 modelVersion。
- 建立统一 `Result<T>` 与 `PageResult<T>`。
- Feed 使用 cardType 判别 payload，公共 attribution 单独建模。
- 未知 cardType、枚举和兼容新增字段使用 safe fallback。
- 时间集中使用同一个 ISO parser，不在各页面重复手写。
- 相对资源 URL 通过 API base URL resolver 处理。
- 不把所有 Feed payload 强转成一个 Model。

## 21. 前端建议开发顺序

1. API Client、Result、PageResult、错误处理、JWT 拦截器。
2. Auth。
3. Onboarding。
4. Feed 六类卡片和未知类型降级。
5. Search。
6. Content、Comment、Interaction、Upload。
7. Team Detail。
8. Player Detail。
9. Match Detail。
10. Ranking。
11. User Center 与公开主页。
12. Notification。
13. 推荐 attribution 与 behavior reporter。
14. 整体联调、空态、错误态和 Token 过期流程。

## 22. 联调常见坑

- MySQL/Redis 未启动导致 Java 启动或 Health 失败。
- Python 未启动并不等于 Feed 不可用；Java 应规则降级。
- 8080/8100 端口冲突或旧进程占用。
- JWT 未携带、Bearer 错误、Token 过期后未清理。
- 写死 Demo ID，清库后全部 404。
- Java Long 被当成 32 位整数。
- null 与空数组混用。
- CURRENT_STANDING 被显示为赛前排名。
- 接口返回时就上报 EXPOSE。
- 详情跳转后丢失 algorithm/request/impression attribution。
- 未知 cardType 或新增可选字段导致 crash。
- 使用历史 CONTENT_CARD/MATCH_CARD 枚举。
- 相对图片 URL 没拼 Spring Boot base URL。
- 将公开用户 favorites/comments 错误做成匿名接口。

## 23. 明确延期与不要依赖

Backend V1 不提供或不承诺：私信/IM、真实足球数据 Provider、转会中心/转会卡、比赛视频、裁判评分、复杂杯赛树、高级热区图、传球图、实时 WebSocket 比分、Push、微信/手机号第三方登录、完整约球模块、广告/支付/会员，以及 Freeze 中其他选做或延期模块。

Flutter V1 不应建立依赖这些能力才能完成的主流程。

## 24. Backend V1 后续变更规则

- Bug：可以修复并补回归验证。
- Flutter 必需字段缺失：只做向后兼容新增。
- 新产品需求：进入后续版本，不破坏 V1。

不随意修改 path、字段名、已有枚举、分页结构或字段语义。新增字段在 Flutter Model 中默认可选。

## 25. 已发现的文档与代码差异

以当前冻结代码为准：

1. 部分冻结说明写过 Result 顶层 timestamp，实际为 code/message/data/traceId，没有 timestamp。
2. 早期 API 示例使用 CONTENT_CARD、MATCH_CARD，当前 cardType 为 CONTENT、MATCH，另有四类卡片。
3. 历史 Feed 说明出现 mixed、match tab，当前冻结 tab 为 recommend/news/following/team。
4. 产品表述曾出现“5 支上限”，当前关注服务没有球队关注数量上限，也没有通用关注上限。
5. Match Overview 的 CURRENT_STANDING 是当前积分榜，不是赛前历史快照。

这些不是本轮 API 修改，只是把真实代码事实明确交给前端。

## 26. 文档索引

- [BACKEND_API_FREEZE_V1.md](./BACKEND_API_FREEZE_V1.md)：冻结范围和契约原则。
- [06_API_SPEC.md](./06_API_SPEC.md)：完整 API 规格和历史说明；冲突时以冻结代码及本文差异清单为准。
- [07_AUTH_SECURITY.md](./07_AUTH_SECURITY.md)：JWT、权限和安全边界。
- [11_VALIDATION_AND_SMOKE_GUIDE.md](./11_VALIDATION_AND_SMOKE_GUIDE.md)：验证、Smoke 和联调方法。
- [T22_BACKEND_V1_FINAL_CLOSURE_REPORT.md](./audits/T22_BACKEND_V1_FINAL_CLOSURE_REPORT.md)：最终闭环、测试结果和冻结依据。
- [FRONTEND_BACKEND_QUICKSTART_V1.md](./FRONTEND_BACKEND_QUICKSTART_V1.md)：前端首次接手的 5 分钟启动入口。
