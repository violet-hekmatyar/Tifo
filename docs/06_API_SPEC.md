# 南看台 App / 后台接口规范

> 版本：v0.2-tifo-revised  
> 定位：定义南看台第一版 HTTP API 规范，包括路径、请求方式、统一返回、分页、错误码、App 端接口和管理后台接口。

## 1. API 设计原则

1. App 端和管理端接口分开。
2. 前端不直接接触数据库字段。
3. Controller 不返回 Entity。
4. 所有接口返回统一结构。
5. 列表接口使用统一分页。
6. 后台接口统一需要管理员权限。
7. 第一版接口优先稳定，不追求复杂 REST 纯粹性。
8. 首页卡片流必须支持后续扩展卡片类型。
9. 原型中未确认的功能可以预留接口，但不强制实现完整业务。

## 2. Base URL 与前缀

本地开发：

```text
http://localhost:8080
```

接口前缀：

| 类型 | 前缀 |
|---|---|
| 认证接口 | `/api/auth/**` |
| App 端接口 | `/api/app/**` |
| 管理端接口 | `/api/admin/**` |
| 公共接口 | `/api/public/**` |
| 文件接口 | `/api/file/**` |
| 内部接口 | `/api/internal/**` |

## 3. 通用请求头

| Header | 必须 | 说明 |
|---|---:|---|
| `Content-Type` | 是 | JSON 请求使用 `application/json` |
| `Authorization` | 登录后必须 | `Bearer <access_token>` |
| `X-Request-Id` | 否 | 请求 ID |
| `X-Trace-Id` | 否 | 链路追踪 ID |

## 4. 统一返回结构

成功：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "trace_demo_001"
}
```

失败：

```json
{
  "code": 40001,
  "message": "参数错误",
  "data": null,
  "traceId": "trace_demo_001"
}
```

## 5. 统一分页

请求参数：

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---:|---|
| `pageNum` | number | 1 | 页码 |
| `pageSize` | number | 10 | 每页数量，最大 100 |

响应结构：

```json
{
  "records": [],
  "total": 100,
  "pageNum": 1,
  "pageSize": 10,
  "pages": 10
}
```

## 6. 通用错误码

| code | message | 说明 |
|---:|---|---|
| 0 | success | 成功 |
| 40001 | 参数错误 | 参数缺失或格式错误 |
| 40101 | 未登录 | Token 缺失或无效 |
| 40102 | 登录已过期 | Token 过期 |
| 40103 | 登录失败次数过多 | 简单防刷 |
| 40301 | 无权限 | 权限不足 |
| 40401 | 资源不存在 | 查询对象不存在 |
| 40901 | 数据冲突 | 重复关注、重复点赞等 |
| 40902 | 关注球队数量已达上限 | 预留错误码；当前 T04 不启用球队关注数量上限 |
| 50001 | 系统异常 | 服务端异常 |

---

# 7. 认证与首次登录接口

## 7.1 注册

```http
POST /api/auth/register
```

请求：

```json
{
  "username": "test_user",
  "phone": "13900000001",
  "password": "123456"
}
```

## 7.2 登录

```http
POST /api/auth/login
```

响应：

```json
{
  "accessToken": "jwt_token_demo",
  "tokenType": "Bearer",
  "expiresIn": 604800,
  "user": {
    "id": 10002,
    "username": "test_user",
    "nickname": "南看台老球迷",
    "roleType": "USER",
    "onboardingCompleted": false
  }
}
```

## 7.3 当前用户

```http
GET /api/auth/me
```

## 7.4 首次登录偏好保存

```http
POST /api/app/onboarding/preferences
```

请求：

```json
{
  "mainTeamId": 30001,
  "followTeamIds": [30001, 30002],
  "followPlayerIds": [40001, 40002]
}
```

响应：

```json
{
  "completed": true,
  "mainTeamId": 30001,
  "followTeamCount": 2,
  "followPlayerCount": 2
}
```

## 7.5 首次登录推荐选择项

```http
GET /api/app/onboarding/options
```

返回推荐球队、热门球队、推荐球员、热门球员。

---

# 8. 首页卡片流接口

## 8.1 首页 feed

```http
GET /api/app/feed
```

参数：

| 参数 | 类型 | 必须 | 说明 |
|---|---|---:|---|
| `tab` | string | 否 | `recommend` / `news` / `following` / `team` |
| `teamId` | number | 否 | 我关注的球队 tab 选中某队时传 |
| `pageNum` | number | 否 | 页码 |
| `pageSize` | number | 否 | 页大小 |

统一卡片返回：

```json
{
  "records": [
    {
      "cardType": "CONTENT_CARD",
      "targetType": "CONTENT",
      "targetId": 20001,
      "sortScore": 98.5,
      "payload": {
        "contentType": "REPORT",
        "contentFormat": "ARTICLE_FORMAT",
        "title": "巴萨客场 3-1 击败马竞",
        "coverUrl": "/uploads/content/barca-report-cover.jpg",
        "hotComment": "这场巴萨的边路推进很明显。",
        "author": {
          "userId": 10001,
          "nickname": "南看台编辑部",
          "avatarUrl": "/uploads/avatar/admin.png",
          "verified": true
        },
        "likeCount": 88,
        "commentCount": 24,
        "publishTime": "2026-06-23 09:00:00"
      }
    },
    {
      "cardType": "MATCH_CARD",
      "targetType": "MATCH",
      "targetId": 50001,
      "payload": {
        "leagueName": "欧冠",
        "homeTeamName": "巴塞罗那",
        "awayTeamName": "拜仁",
        "homeScore": 1,
        "awayScore": 0,
        "matchStatus": "LIVE",
        "matchTime": "2026-06-23 04:00:00",
        "eventSummary": "63' 莱万进球"
      }
    }
  ],
  "total": 2,
  "pageNum": 1,
  "pageSize": 10,
  "pages": 1
}
```

## 8.2 热门赛事入口

```http
GET /api/app/feed/hot-leagues
```

返回欧冠、英超、西甲、中超等赛事入口。

---

# 9. 内容接口

## 9.1 内容详情

```http
GET /api/app/contents/{contentId}
```

响应要支持：

```text
基础信息
作者信息
媒体列表
图文分段 blocks
关联球队/球员/比赛/热点事件
用户点赞/收藏状态
统计数
```

## 9.2 发布帖子

```http
POST /api/app/contents/posts
```

请求：

```json
{
  "title": "这场巴萨的边路推进值得聊聊",
  "body": "我觉得这场比赛关键在于右路拉开空间……",
  "mediaUrls": ["/uploads/content/user-post-1.jpg"],
  "relationList": [
    {"relationType": "TEAM", "relationId": 30001},
    {"relationType": "MATCH", "relationId": 50001}
  ]
}
```

## 9.3 后台发布文章/战报

见管理后台接口。

---

# 10. 评论、点赞、收藏接口

## 10.1 评论列表

```http
GET /api/app/comments
```

参数：

| 参数 | 类型 | 必须 | 说明 |
|---|---|---:|---|
| `targetType` | string | 是 | `CONTENT` / `MATCH` / `PLAYER_RATING` |
| `targetId` | number | 是 | 目标 ID |
| `sort` | string | 否 | `hot` / `time` |

## 10.2 发表评论

```http
POST /api/app/comments
```

请求：

```json
{
  "targetType": "CONTENT",
  "targetId": 20001,
  "parentId": 0,
  "contentText": "这场比赛节奏确实很快。"
}
```

## 10.3 点赞 / 取消点赞

```http
POST /api/app/likes/toggle
```

## 10.4 收藏 / 取消收藏

```http
POST /api/app/favorites/toggle
```

---

# 11. 足球数据接口

## 11.1 数据页：重要比赛

```http
GET /api/app/football/matches/important
```

参数：`date`、`pageNum`、`pageSize`。

## 11.2 数据页：关注球队赛程

```http
GET /api/app/football/matches/following-teams
```

参数：`teamId` 可选，不传则返回所有关注球队赛程。

## 11.3 赛事列表

```http
GET /api/app/football/leagues
```

## 11.4 比赛列表

```http
GET /api/app/football/matches
```

参数：`leagueId`、`teamId`、`date`、`status`、`pageNum`、`pageSize`。

## 11.5 积分榜

```http
GET /api/app/football/standings
```

参数：`leagueId`、`season`、`stage`。

## 11.6 球员榜

```http
GET /api/app/football/player-ranks
```

参数：`leagueId`、`season`、`rankType`。

## 11.7 球队榜

```http
GET /api/app/football/team-ranks
```

---

# 12. 球队详情接口

## 12.1 球队详情头部

```http
GET /api/app/football/teams/{teamId}
```

## 12.2 球队总览

```http
GET /api/app/football/teams/{teamId}/overview
```

返回：最近赛程、排名、最热资讯、队内榜单、球队信息、球队荣誉。

## 12.3 球队帖子

```http
GET /api/app/football/teams/{teamId}/contents
```

## 12.4 球队球员

```http
GET /api/app/football/teams/{teamId}/players
```

按位置分类返回：前锋、中场、后卫、门将、教练。

## 12.5 球队数据

```http
GET /api/app/football/teams/{teamId}/stats
```

参数：`seasonCompetitionId`、`mode=total|average`。

## 12.6 球队赛程

```http
GET /api/app/football/teams/{teamId}/matches
```

---

# 13. 球员详情接口

## 13.1 球员头部

```http
GET /api/app/football/players/{playerId}
```

## 13.2 球员总览

```http
GET /api/app/football/players/{playerId}/overview
```

## 13.3 球员帖子

```http
GET /api/app/football/players/{playerId}/contents
```

## 13.4 球员数据

```http
GET /api/app/football/players/{playerId}/stats
```

参数：`seasonCompetitionId`、`mode=total|average`。

## 13.5 球员比赛

```http
GET /api/app/football/players/{playerId}/matches
```

要支持俱乐部和国家队比赛。

## 13.6 球员生涯

```http
GET /api/app/football/players/{playerId}/career
```

按球队维度和赛季维度返回俱乐部/国家队数据。

---

# 14. 比赛详情接口

## 14.1 比赛详情头部

```http
GET /api/app/football/matches/{matchId}
```

## 14.2 比赛总览

```http
GET /api/app/football/matches/{matchId}/overview
```

返回：比赛战报、全场集锦外链、简要统计、关键事件。

## 14.3 比赛评分 tab

```http
GET /api/app/football/matches/{matchId}/ratings
```

参数：`side=home|away|other`。

## 14.4 球员评分详情

```http
GET /api/app/football/matches/{matchId}/players/{playerId}/rating-detail
```

返回：球员本场 8 项关键数据、平均评分、用户自己的评分、评分分布、评论列表入口。

## 14.5 提交球员评分

```http
POST /api/app/football/matches/{matchId}/players/{playerId}/rating
```

请求：

```json
{
  "ratingScore": 8.5
}
```

说明：同一用户重复评分覆盖上一次。

## 14.6 比赛阵容

```http
GET /api/app/football/matches/{matchId}/lineup
```

## 14.7 比赛排名

```http
GET /api/app/football/matches/{matchId}/ranking
```

## 14.8 比赛统计

```http
GET /api/app/football/matches/{matchId}/stats
```

---

# 15. 关注接口

## 15.1 关注 / 取消关注

```http
POST /api/app/follows/toggle
```

请求：

```json
{
  "followType": "TEAM",
  "targetId": 30001,
  "isMain": true
}
```

响应：

```json
{
  "followed": true,
  "followType": "TEAM",
  "targetId": 30001,
  "relationStatus": "FOLLOWED"
}
```

## 15.2 我的关注

```http
GET /api/app/follows/my
```

参数：`followType` 可选，取值 `USER` / `TEAM` / `PLAYER`。

## 15.3 粉丝列表

```http
GET /api/app/follows/followers
```

响应中的 `relationStatus`：

```text
FOLLOW_BACK / FOLLOWED / MUTUAL
```

---

# 16. 我的页面接口

```http
GET /api/app/users/me/profile
GET /api/app/users/me/stand
GET /api/app/users/me/contents?type=post
GET /api/app/users/me/likes
GET /api/app/users/me/favorites
GET /api/app/users/me/comments
```

`stand` 返回主队、关注球队、关注球员。

---

# 17. 消息接口

```http
GET  /api/app/messages
POST /api/app/messages/{messageId}/read
POST /api/app/messages/read-all
```

第一版消息类型：`SYSTEM`、`COMMENT`、`LIKE`、`FOLLOW`、`RATING`。  
私信聊天接口暂缓。

---

# 18. 约球接口预留

约球 P2 预留，不作为第一轮主线。

```http
GET  /api/app/pickup-activities
GET  /api/app/pickup-activities/{activityId}
POST /api/app/pickup-activities/{activityId}/join
POST /api/app/pickup-activities/{activityId}/cancel
```

---

# 19. 管理后台接口

## 19.1 内容管理

```http
GET    /api/admin/contents
POST   /api/admin/contents
PUT    /api/admin/contents/{contentId}
POST   /api/admin/contents/{contentId}/offline
DELETE /api/admin/contents/{contentId}
```

## 19.2 足球数据管理

```http
GET    /api/admin/football/leagues
POST   /api/admin/football/leagues
PUT    /api/admin/football/leagues/{leagueId}

GET    /api/admin/football/teams
POST   /api/admin/football/teams
PUT    /api/admin/football/teams/{teamId}

GET    /api/admin/football/players
POST   /api/admin/football/players
PUT    /api/admin/football/players/{playerId}

GET    /api/admin/football/matches
POST   /api/admin/football/matches
PUT    /api/admin/football/matches/{matchId}

POST   /api/admin/football/matches/{matchId}/events
POST   /api/admin/football/matches/{matchId}/stats
POST   /api/admin/football/standings
```

## 19.3 用户管理

```http
GET  /api/admin/users
POST /api/admin/users/{userId}/disable
POST /api/admin/users/{userId}/enable
```

---

# 20. 文件上传接口

```http
POST /api/file/upload
Content-Type: multipart/form-data
```

响应：

```json
{
  "fileUrl": "/uploads/2026/06/demo.jpg",
  "fileName": "demo.jpg",
  "fileSize": 102400,
  "fileType": "image/jpeg"
}
```

第一版文件存储：Docker volume / 本地 uploads 目录。后期可替换为 MinIO / OSS。
# T05 Current Implementation Notes

Implemented in T05:

```http
GET  /api/app/contents/{contentId}
POST /api/app/contents/posts
GET  /api/app/comments
POST /api/app/comments
POST /api/app/likes/toggle
POST /api/app/favorites/toggle
```

Current scope:

```text
Content detail returns VO data, author, mediaList, relationList, counts, liked and favorited.
Anonymous content detail and comment list are public; liked/favorited/comment liked flags default to false.
Post creation creates POST / POST_FORMAT / CONTENT_CARD / USER / PUBLISHED records.
Post body may be blank only when mediaUrls contains at least one URL.
Comment list supports CONTENT targets, root comments, second-level replies, hot/time sorting, PageResult.
Like toggle supports CONTENT and COMMENT.
Favorite toggle supports CONTENT only.
```

# T06 Current Implementation Notes

Implemented in T06:

```http
GET /api/app/football/leagues
GET /api/app/football/matches/important
GET /api/app/football/matches/following-teams
GET /api/app/football/matches
GET /api/app/football/teams/{teamId}
GET /api/app/football/players/{playerId}
GET /api/app/football/matches/{matchId}
```

Current scope:

```text
League list returns active leagues.
Important match list uses important_level and returns PageResult records.
Following-team schedule requires login and uses the current user's active TEAM follow records.
Match list supports leagueId, teamId, date, and status filters.
Team detail returns basic info, followed flag, recent matches, and upcoming matches.
Player detail returns basic info, current team, age, and followed flag.
Match detail returns basic match data, sorted eventList, and report entry when present.
```

Out of scope for T06:

```text
Home feed or recommendation.
Full team/player tabs.
Lineups, ratings, standings, ranks, and complex match stats.
Admin football CRUD.
Realtime scores, WebSocket, or third-party sports API integration.
```

# T07 Current Implementation Notes

Implemented in T07:

```http
GET /api/app/feed
GET /api/app/feed/hot-leagues
```

`GET /api/app/feed` supports:

```text
tab=recommend|following|news|match|mixed
pageNum, pageSize, cursor
leagueId, teamId
```

Current scope:

```text
CONTENT and MATCH cards are returned in one feed response.
Anonymous recommend/mixed returns hot content and important matches.
JWT recommend adds main-team, followed-team, and followed-player rule boosts.
following without JWT returns an empty page; following with JWT returns related content and matches.
news returns NEWS, ARTICLE, and REPORT content only.
match returns MATCH cards only.
hot-leagues returns active leagues with live/upcoming match counts and a simple hotScore.
pageSize is capped at 100.
```

Out of scope for T07:

```text
Algorithm service integration.
Vector search or machine learning recommendation.
Search API.
Admin CRUD.
Realtime score push.
Third-party sports API.
```

# T08 Current Implementation Notes

Implemented in T08:

```http
GET  /api/app/users/me/summary
PUT  /api/app/users/me/profile
GET  /api/app/users/me/contents
GET  /api/app/users/me/favorites
GET  /api/app/users/me/comments
GET  /api/admin/dashboard/summary
GET  /api/admin/users
PUT  /api/admin/users/{userId}/status
GET  /api/admin/contents
PUT  /api/admin/contents/{contentId}/status
```

Current scope:

```text
User summary aggregates profile, main team, follow counts, post count, favorite count, comment count, and three recent lists.
Profile update only writes user_profile fields: nickname, avatarUrl, bio, and mainTeamId.
My contents returns current-user authored content with PageResult.
My favorites returns current-user active CONTENT favorites and filters hidden/deleted content from user-side output.
My comments returns current-user active comments with target title when the target is CONTENT.
Admin dashboard returns realtime MySQL counters.
Admin users returns PageResult user VOs with masked phone and no password hash.
Admin user status supports ACTIVE and DISABLED.
Admin contents returns PageResult content VOs without body.
Admin content status supports PUBLISHED and HIDDEN.
```

Out of scope for T08:

```text
Full RBAC, menu permissions, button permissions, departments, and org trees.
Rich text editor, content audit workflow, file upload, export, dashboard screens, notifications, third-party login, and refresh tokens.
```

# T09 Current Implementation Notes

Implemented in T09:

```http
POST /api/app/files/upload
GET  /api/public/files/{fileId}
```

Upload scope:

```text
Requires login.
multipart/form-data parameters: file, bizType.
bizType values: AVATAR, CONTENT_IMAGE, COMMENT_IMAGE, GENERAL_IMAGE.
Returns fileId, url, bizType, originalName, contentType, extension, and sizeBytes.
Default max size: 10MB.
Allowed extensions: jpg, jpeg, png, webp, gif.
Allowed content types: image/jpeg, image/png, image/webp, image/gif.
```

Public file access:

```text
Public binary response.
Reads metadata by fileId, then resolves relative_path under storage-root.
Returns 404 when metadata is missing, deleted, non-ACTIVE, outside storage-root, or missing on disk.
Sets Content-Type and X-Content-Type-Options: nosniff.
```

Out of scope for T09:

```text
Video upload
Chunk upload
OSS / MinIO / object storage
Image crop/compress/watermark
Private file authorization
```

# T10 Current Implementation Notes

Implemented in T10:

```http
POST   /api/app/users/me/avatar
DELETE /api/app/files/{fileId}
POST   /api/app/contents/posts
```

Storage abstraction:

```text
StorageService defines store/load/delete.
LocalStorageService is the default LOCAL implementation.
AliyunOssStorageService, QiniuKodoStorageService, and MinioStorageService are placeholders and do not introduce SDK dependencies.
StorageServiceResolver chooses the upload storage by app.file.storage-type and historical file reads by file_resource.storage_type.
Switching to an unimplemented storage type returns a clear server-side capability error instead of silently falling back to LOCAL.
```

Avatar binding:

```text
POST /api/app/users/me/avatar
Body: {"fileId": 90001}
The file must be ACTIVE, owned by the current user, and bizType AVATAR or GENERAL_IMAGE.
The service updates user_profile.avatar_url to file.url.
```

Content media binding:

```text
POST /api/app/contents/posts supports mediaFileIds in addition to mediaUrls.
Each media file must be ACTIVE, owned by the current user, and bizType CONTENT_IMAGE or GENERAL_IMAGE.
The service writes content_media rows using file.url.
```

Soft delete:

```text
DELETE /api/app/files/{fileId}
Only the owning user can delete.
Sets file_resource.status=DELETED and deleted=1.
Physical files are retained for later cleanup.
Deleted public files return 404.
```

# T11 Current Implementation Notes

Implemented in T11:

```http
GET    /api/app/users/{userId}/profile
POST   /api/app/users/{userId}/follow
DELETE /api/app/users/{userId}/follow
GET    /api/app/users/{userId}/followings
GET    /api/app/users/{userId}/followers
GET    /api/app/users/{userId}/contents
GET    /api/app/users/{userId}/favorites
GET    /api/app/users/{userId}/comments
GET    /api/app/users/me/stand
GET    /api/app/feed?tab=following
```

User follows reuse `follow_record` with `follow_type=USER`; there is no user follow cap. Public profile/list/content reads are available without login; when a bearer token is present, `relationStatus` is calculated for that viewer. Favorites and comments under `/api/app/users/{userId}/...` are self-only in this version and return `40301` for other users. DISABLED or deleted users are not visible and cannot actively follow.

`relationStatus` values: `SELF`, `NONE`, `FOLLOWING`, `FOLLOWED_BY`, `MUTUAL`.

`GET /api/app/feed?tab=following` now includes content authored by followed users in addition to followed teams, followed players, and related matches. If no personalized cards are found, it falls back to the normal mixed feed behavior.

# T12 Current Implementation Notes

Implemented in T12:

```http
GET    /api/app/comments?contentId={contentId}&sort=hot&pageNum=1&pageSize=20
GET    /api/app/comments?contentId={contentId}&sort=latest&pageNum=1&pageSize=20
GET    /api/app/comments/{commentId}/replies?pageNum=1&pageSize=20&sort=latest
GET    /api/app/comments/hot?contentId={contentId}&limit=3
POST   /api/app/comments
POST   /api/app/comments/{commentId}/likes/toggle
DELETE /api/app/comments/{commentId}
```

Compatibility:

```text
The old list format targetType=CONTENT&targetId=... still works.
The old create format targetType/targetId/contentText still works.
The new create format contentId/content/replyToUserId is supported.
```

Comment sorting:

```text
sort=latest orders by create_time DESC.
sort=hot uses heatScore = likeCount + replyCount * 2.
finalScore = heatScore * timeFactor.
timeFactor: <=2h 1.5, <=12h 1.0, <=24h 0.7, >24h 0.3.
```

Replies:

```text
rootId points to the root comment.
parentId stores the directly replied comment.
replyToUserId/replyToNickname identify the displayed reply target.
Root comment list includes a small reply preview. The replies endpoint returns paged second-level replies under the root.
```

Hot comment:

```text
GET /api/app/comments/hot returns ACTIVE root comments sorted by hot score.
Feed content cards include hotComment when an ACTIVE root comment exists.
```

Soft delete:

```text
Authors can delete their own comments. ADMIN can delete any comment.
Delete sets status=DELETED and is_deleted=1.
Deleted comments are excluded from normal lists, cannot be liked, and cannot be replied to.
```
