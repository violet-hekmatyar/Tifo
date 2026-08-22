# 南看台 / Tifo

南看台 / Tifo 是一个卡片化足球内容流 + 赛事数据 + 社区互动 APP。

当前阶段：T03 登录鉴权与用户闭环，已具备注册、登录、JWT、当前用户和管理员权限 smoke 闭环。

当前技术路线：

```text
Spring Boot 3.2.4 + JDK 17 + MySQL + Redis + MyBatis-Plus + Spring Security/JWT + Knife4j
```

当前开发方式：

```text
Windows 本地开发
Linux 服务器当前阶段优先采用 jar 直跑
MySQL / Redis 在 Linux 上已准备好，后端运行时通过 127.0.0.1 连接
```

文档入口：

```text
docs/00_DOCUMENT_MAP.md
```

安全约束：

```text
不要提交真实密码、真实服务器 IP、真实 Token、JWT Secret、.env 或 application-prod.yml。
```

## 本地构建

```powershell
mvn clean test
mvn clean package
```

打包产物：

```text
target/south-stand-server.jar
```

## 本地运行

```powershell
java -jar .\target\south-stand-server.jar
```

健康检查：

```powershell
Invoke-RestMethod http://localhost:8080/api/public/health
```

Knife4j 页面：

```text
http://localhost:8080/doc.html
```

## T01 验收脚本

```powershell
.\scripts\windows\check-t01.ps1
```

该脚本会执行 `mvn clean test`、`mvn clean package`、启动 jar，并请求 `/api/public/health`。

## T02 本地数据库与 Redis 检查

T02 已接入 MySQL / Redis 基础连接，并提供：

```text
GET /api/public/health
GET /api/public/health/db
GET /api/public/health/redis
```

重置本地开发库：

```powershell
.\scripts\windows\reset-dev-db.ps1 -ConfirmReset -ConfirmationText "RESET south_stand"
```

T02 验收：

```powershell
.\scripts\windows\check-t02.ps1
```

本地连接参数通过环境变量覆盖：

```text
MYSQL_HOST / MYSQL_PORT / MYSQL_DATABASE / MYSQL_USERNAME / MYSQL_PASSWORD
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
```

## T03 登录鉴权检查

T03 已提供：

```text
POST /api/auth/register
POST /api/auth/login
GET /api/auth/me
GET /api/admin/health
```

JWT 配置通过环境变量覆盖：

```text
JWT_SECRET
JWT_ACCESS_TOKEN_EXPIRE_SECONDS
```

T03 验收：

```powershell
.\scripts\windows\check-t03.ps1
```

## T04 首次登录与关注检查

T04 已提供：

```text
GET /api/app/onboarding/options
POST /api/app/onboarding/preferences
POST /api/app/follows/toggle
GET /api/app/users/me/profile
```

当前关注球队不设置数量上限。首次登录保存偏好时，`mainTeamId` 会自动加入关注球队。

T04 验收：

```powershell
.\scripts\windows\check-t04.ps1
```
## T05 Content And Interaction Check

T05 provides:

```text
GET /api/app/contents/{contentId}
POST /api/app/contents/posts
GET /api/app/comments
POST /api/app/comments
POST /api/app/likes/toggle
POST /api/app/favorites/toggle
```

Public reads are allowed for content detail and comment list. Post creation, comment creation, like toggle, and favorite toggle require JWT login.

T05 validation:

```powershell
.\scripts\windows\check-t05.ps1
```

## T06 Football Data Check

T06 provides:

```text
GET /api/app/football/leagues
GET /api/app/football/matches/important
GET /api/app/football/matches/following-teams
GET /api/app/football/matches
GET /api/app/football/teams/{teamId}
GET /api/app/football/players/{playerId}
GET /api/app/football/matches/{matchId}
```

League, match list, team detail, player detail, and match detail reads are public. Following-team match schedule requires JWT login.

T06 validation:

```powershell
.\scripts\windows\check-t06.ps1
```

## T07 Feed Rule Recommendation Check

T07 provides:

```text
GET /api/app/feed
GET /api/app/feed/hot-leagues
```

The feed is public and supports optional JWT personalization. The first version uses simple rule scores across content cards and match cards for `recommend`, `following`, `news`, `match`, and `mixed` tabs.

T07 validation:

```powershell
.\scripts\windows\check-t07.ps1
```

## T08 User Center And Admin Basic Check

T08 provides:

```text
GET /api/app/users/me/summary
PUT /api/app/users/me/profile
GET /api/app/users/me/contents
GET /api/app/users/me/favorites
GET /api/app/users/me/comments
GET /api/admin/dashboard/summary
GET /api/admin/users
PUT /api/admin/users/{userId}/status
GET /api/admin/contents
PUT /api/admin/contents/{contentId}/status
```

All `/api/app/users/me/**` endpoints require login. All `/api/admin/**` endpoints require an ADMIN token. Admin status changes write best-effort operation logs.

T08 validation:

```powershell
.\scripts\windows\check-t08.ps1
```

## T09 File Upload Security Check

T09 provides:

```text
POST /api/app/files/upload
GET /api/public/files/{fileId}
```

The first version supports local disk image uploads only: `jpg`, `jpeg`, `png`, `webp`, and `gif`, default max size 10MB. Uploaded files are stored under `uploads/` or `APP_FILE_STORAGE_ROOT`; `uploads/` is ignored by Git. Public responses expose only `/api/public/files/{fileId}`, never a local disk path.

T09 validation:

```powershell
.\scripts\windows\check-t09.ps1
```

## T10 Storage Abstraction And Media Binding Check

T10 keeps the T09 upload API compatible and adds:

```text
POST /api/app/users/me/avatar
DELETE /api/app/files/{fileId}
POST /api/app/contents/posts with mediaFileIds
```

File storage now goes through `StorageService`. `LOCAL` is the default implementation; `ALIYUN_OSS`, `QINIU_KODO`, and `MINIO` are registered placeholders without SDK dependencies.

T10 validation:

```powershell
.\scripts\windows\check-t10.ps1
```

## T11 User Social Check

T11 provides:

```text
GET    /api/app/users/{userId}/profile
POST   /api/app/users/{userId}/follow
DELETE /api/app/users/{userId}/follow
GET    /api/app/users/{userId}/followings
GET    /api/app/users/{userId}/followers
GET    /api/app/users/{userId}/contents
GET    /api/app/users/{userId}/favorites
GET    /api/app/users/{userId}/comments
GET    /api/app/users/me/stand
```

User follows reuse `follow_record` with `follow_type=USER`; there is no user follow cap. Public profile/list/content endpoints are readable without login and calculate `relationStatus` when a token is provided. Other users' favorites/comments are private in this version. `tab=following` feed now includes followed users' content.

T11 validation:

```powershell
.\scripts\windows\check-t11.ps1
```

## T12 Comment Hot Check

T12 provides:

```text
GET    /api/app/comments?contentId={contentId}&sort=hot
GET    /api/app/comments?contentId={contentId}&sort=latest
GET    /api/app/comments/{commentId}/replies
GET    /api/app/comments/hot?contentId={contentId}&limit=3
POST   /api/app/comments
POST   /api/app/comments/{commentId}/likes/toggle
DELETE /api/app/comments/{commentId}
```

Hot score uses `(likeCount + replyCount * 2) * timeFactor`, where the time factor is `1.5` for comments up to 2 hours old, `1.0` up to 12 hours, `0.7` up to 24 hours, and `0.3` after 24 hours. Feed content cards now include `hotComment` when one is available.

T12 validation:

```powershell
.\scripts\windows\check-t12.ps1
```

## T13 Likes, Article Blocks, And Entity Search Check

T13 provides:

```text
GET    /api/app/users/me/likes
POST   /api/app/contents/articles
PUT    /api/app/contents/{contentId}/articles
GET    /api/app/search/entities
```

`GET /api/app/users/me/likes` returns current-user active CONTENT likes only; comment likes stay in `like_record` but are not mixed into this page. ARTICLE uses ordered `content_block` rows for TEXT and IMAGE blocks while keeping `content.body`, `cover_url`, `mediaList`, and old POST/detail APIs compatible. Entity search is public and covers TEAM, PLAYER, and MATCH. Follow limits are not enforced for USER, TEAM, or PLAYER follows; the historical "5 teams" idea is not active.

T13 validation:

```powershell
.\scripts\windows\check-t13.ps1
```

## T14 Deterministic Demo Dataset

T14 adds a Chinese-first, fully offline demo dataset with stable IDs, times, relationships, counters, and locally generated SVG assets. It is synthetic development data and must never be imported automatically in production.

```powershell
py -3 scripts/data/generate-demo-data.py
.\scripts\windows\init-demo-data.ps1 -Mode ResetDemo -ConfirmReset
.\scripts\windows\check-t14.ps1
```

Configuration and account details are documented in `scripts/data/README.md`. The generator uses fixed seed `20260722`, has no third-party sports API dependency, and does not impose USER, TEAM, or PLAYER follow limits.
# T15 赛季与榜单

T15 提供联赛赛季、赛事阶段、积分榜、球员榜和球队榜。默认初始化与完整回归都采用增量模式，只补缺失表、索引和 T15 数据，并用 ID + SHA-256 指纹验证既有用户、社区、文件及足球基础数据未改变。主要公开接口为 `/api/app/football/leagues/{leagueId}/seasons`、`/api/app/football/standings`、`/api/app/football/player-ranks` 和 `/api/app/football/team-ranks`。

```powershell
.\scripts\windows\init-demo-data.ps1                  # 默认 Incremental
.\scripts\windows\check-t15.ps1                      # 默认非破坏性
.\scripts\windows\check-t15.ps1 -ResetDatabase -ConfirmReset  # 显式破坏性重建
```

## T16 球队与球员详情

T16 增加赛季阵容、球队荣誉和可证明的球员当前效力历史，并通过 T15 统计聚合球队详情、球员详情和生涯汇总。默认初始化及检查保持非破坏性：

```powershell
py -3 scripts/data/generate-demo-data.py --scope t16 --mode incremental
.\scripts\windows\init-t16-data.ps1
.\scripts\windows\check-t16.ps1
```
## T17 比赛阵容、技术统计与球员评分

T17 新增比赛阵容、出场、球队技术统计、球员技术统计和用户球员评分模型，并扩展比赛详情可用性字段。公开读取接口位于 `/api/app/football/matches/{matchId}` 下；评分 POST/DELETE 需要 JWT。评分范围为 1.0-10.0、步长 0.5，只允许已结束比赛的实际出场球员。

执行 `scripts/windows/init-t17-data.ps1` 可进行非破坏迁移、Demo 阵容扩充、增量比赛数据生成和一致性校验。脚本使用 T17 专用 ID 为 9 支目标队各补齐 18 人（2 门将、6 后卫、6 中场、4 前锋）的五层关系，并生成 20 场完整比赛、300 条有效评分；不会修改旧比分、事件或非 DEMO 数据。
# T18 推荐系统融合

当前后端在保留 T14 批量 Feed 装载的基础上增加 RULE_V2、CONTENT Item-CF、稳定 A/B、7:3 内容/比赛混排、推荐归因、行为闭环与三级降级。Python 服务位于 `recommend-service`，关闭时 Java Feed 自动使用 RULE_V2。执行 `scripts/windows/check-t18.ps1` 完成核心验收，HTTP 专项使用 `scripts/windows/smoke-recommendation.ps1`。
