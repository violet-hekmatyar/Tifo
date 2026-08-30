# Backend API Freeze V1

> 冻结日期：2026-08-30。Flutter 第一版以本文和 `docs/06_API_SPEC.md` 的 T22 口径开发；后端只允许兼容新增字段，不改已有字段含义、枚举或路径。

## 1. 第一版模块与路径

| 模块 | 主要路径 |
|---|---|
| 认证与首次登录 | `/api/auth/register`、`/api/auth/login`、`/api/auth/me`、`/api/app/onboarding/**` |
| 首页 | `GET /api/app/feed`、`GET /api/app/feed/hot-leagues`、`GET /api/app/search/entities` |
| 内容与互动 | `/api/app/contents/**`、`/api/app/comments/**`、`/api/app/likes/toggle`、`/api/app/favorites/toggle` |
| 足球数据 | `/api/app/football/leagues/**`、`/matches/**`、`/standings`、`/player-ranks`、`/team-ranks` |
| 球队/球员详情 | `/api/app/football/teams/{teamId}/**`、`/api/app/football/players/{playerId}/**` |
| 用户中心 | `/api/app/users/me/**`、`/api/app/users/{userId}/**`、`/api/app/follows/**` |
| 通知 | `/api/app/notifications`、`/unread-count`、`/{notificationId}/read`、`/read-all` |
| 推荐行为 | `POST /api/app/recommendation/behaviors/batch` |
| 管理后台 | `/api/admin/**`；推荐指标为 `/api/internal/recommendation/metrics` |

## 2. 通用契约

- 所有业务响应使用 `Result`：`code/message/data/timestamp`；成功码为 `0`。
- 分页统一为 `records/total/pageNum/pageSize/pages`；空页的 `records` 必须是 `[]`。
- 可空单对象使用 `null`；ID 均按 64 位整数处理，Flutter 建议用 `int`，不要转 32 位。
- 时间为后端 JSON 的 ISO 本地日期时间格式；客户端按服务端字符串解析，不自行猜测时区偏移。
- 公开只读接口可匿名访问；个人 Feed、用户动作、用户中心、通知和行为上报要求 Bearer JWT；`/api/admin/**`、`/api/internal/**` 要求 ADMIN。

## 3. 稳定枚举

- Feed `tab`：`recommend/news/following/team`。
- Feed `cardType`：`CONTENT/MATCH/HOT_COMMENT/DISCUSSION/RANKING/PLAYER_RATING`；Flutter 必须允许未知新卡片安全降级。
- 内容：`POST/ARTICLE/NEWS/REPORT`；内容状态对 App 有效值为 `PUBLISHED`。
- 搜索 `entityType`：`TEAM/PLAYER/MATCH/CONTENT`；不传表示四类混合搜索。
- 比赛状态：`SCHEDULED/LIVE/FINISHED`；事件包括 `GOAL/YELLOW_CARD/RED_CARD/SUBSTITUTION`。
- 关注类型：`USER/TEAM/PLAYER`；用户关系：`SELF/NONE/FOLLOWING/FOLLOWED_BY/MUTUAL`。
- 通知类型：`CONTENT_LIKED/CONTENT_COMMENTED/COMMENT_REPLIED/COMMENT_LIKED/USER_FOLLOWED/SYSTEM`。
- 通知 targetType：`CONTENT/COMMENT/USER/SYSTEM`；`readStatus`：`READ/UNREAD`（兼容 `1/0`）。

## 4. Feed 与推荐约束

- 推荐页混排多类型卡片；资讯页只返回资讯/文章语义内容并保留排序；关注页覆盖关注作者、球队和球员关系，候选不足时用热门内容补位。
- 推荐响应携带 `algorithmVersion/modelVersion/experimentId/experimentBucket/requestId`；卡片可携带 `reasonCode/reason/impressionId/position`。
- Flutter 仅在卡片真实进入可视区时上报 `EXPOSE`。Python 不可用时 Java 自动回退 `RULE_V2`，页面不可因推荐服务故障报错。

## 5. Demo 数据与 Flutter 开发约束

- 当前数据为人造 Demo，不代表官方实时数据；关系、一致性和主要页面覆盖已通过 T15-T22 校验。
- Flutter 不依赖固定展示顺序或固定 Demo ID；应使用接口返回 ID、可用性字段和分页元数据。
- `targetAvailable=false` 的通知目标应显示失效态；新增可空字段应保持向后兼容。
- 后端冻结后如需改字段含义、删除字段、改枚举或改路径，必须另开版本；仅兼容新增不破坏 V1。

## 6. 明确延期/选做

私信/IM、真实足球数据 Provider、转会中心、转会卡、比赛视频、裁判评分、复杂杯赛树、高级热区图、高级传球图、实时 WebSocket 比分、Push、微信/手机号第三方登录，以及约球完整模块、广告、支付、会员等选做能力均不进入 Backend V1。
