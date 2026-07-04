# 南看台数据库表结构与样例数据

> 版本：v0.2-tifo-revised  
> 定位：定义南看台第一版后端数据库设计，包括表结构、核心字段、索引、枚举值和样例数据。用于约束 Entity / Mapper / Service 开发、`schema.sql`、`seed.sql` 和 AI Coding。

## 1. 数据库基础规范

数据库名：

```sql
south_stand
```

表命名：

```text
小写下划线
业务域前缀可选
```

主键策略：

```text
BIGINT + MyBatis-Plus ASSIGN_ID
```

外键策略：

```text
第一版不使用数据库物理外键，由业务层保证关联关系。
```

## 2. 通用字段

除点赞、收藏等极简行为表外，核心业务表统一包含：

| 字段 | 类型 | 必须 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 是 | ASSIGN_ID | 主键 |
| `status` | `VARCHAR(32)` | 是 | `ACTIVE` | 状态 |
| `is_deleted` | `TINYINT` | 是 | `0` | 逻辑删除 |
| `extra_json` | `JSON` | 否 | `NULL` | 扩展信息 |
| `remark` | `VARCHAR(512)` | 否 | `NULL` | 备注 |
| `create_time` | `DATETIME` | 是 | `CURRENT_TIMESTAMP` | 创建时间 |
| `update_time` | `DATETIME` | 是 | `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` | 更新时间 |

`extra_json` 使用边界：

```text
可以放低频扩展字段，例如展示配置、临时标签、第三方 API 原始字段、UI 占位配置。
不能放核心查询字段，例如 user_id、team_id、player_id、match_id、status、publish_time。
核心查询字段必须单独建字段并按查询场景建索引。
```

## 3. 索引设计原则

1. 登录字段可建唯一索引，例如 `username`、`phone`，但业务查询不要依赖 username。
2. 所有关联 ID 字段应按查询场景建立索引，例如 `user_id`、`team_id`、`player_id`、`match_id`。
3. 列表查询优先使用组合索引，例如 `status + publish_time`、`league_id + match_time`。
4. 点赞、收藏、关注、报名、评分等行为表要用唯一索引保证幂等。
5. 低区分度字段如 `status`、`type` 不建议单独滥建索引，应放入组合索引。
6. 排序字段如 `publish_time`、`hot_score`、`match_time` 应结合过滤条件建组合索引。

## 4. 状态与枚举

通用状态：

```text
ACTIVE      正常
DISABLED    禁用
PENDING     待处理
PUBLISHED   已发布
DRAFT       草稿
OFFLINE     下架
FINISHED    已结束
CANCELLED   已取消
```

内容类型：

```text
NEWS / POST / ARTICLE / REPORT / DISCUSS
```

卡片类型：

```text
CONTENT_CARD / MATCH_CARD / RANKING_CARD / RATING_CARD / DISCUSSION_CARD / HOT_COMMENT_CARD / TRANSFER_CARD / AD_CARD
```

关注类型：

```text
USER / TEAM / PLAYER
```

比赛状态：

```text
SCHEDULED / LIVE / FINISHED / POSTPONED / CANCELLED
```

比赛事件类型：

```text
GOAL / YELLOW_CARD / RED_CARD / SUBSTITUTION / VAR / PENALTY / OWN_GOAL
```

## 5. 表清单

| 数据域 | 表名 | 说明 | 第一版 |
|---|---|---|---|
| 用户域 | `sys_user` | 用户账号 | P0 |
| 用户域 | `user_profile` | 用户资料、主队 | P0 |
| 用户域 | `user_onboarding` | 首次登录设置状态 | P0 |
| 内容域 | `content` | 资讯、帖子、文章、战报、讨论 | P0 |
| 内容域 | `content_block` | 文章图文分段内容 | P1 |
| 内容域 | `content_media` | 内容图片/视频链接 | P0 |
| 内容域 | `content_relation` | 内容关联球队/球员/比赛/热点事件 | P0/P1 |
| 内容域 | `hot_event` | 热点事件 | P1 |
| 互动域 | `comment` | 评论与二级回复 | P0 |
| 互动域 | `like_record` | 点赞记录 | P0 |
| 互动域 | `favorite_record` | 收藏记录 | P0 |
| 关注域 | `follow_record` | 关注用户/球队/球员 | P0 |
| 足球数据域 | `football_league` | 联赛/赛事 | P0 |
| 足球数据域 | `football_team` | 球队 | P0 |
| 足球数据域 | `football_player` | 球员 | P0 |
| 足球数据域 | `team_player` | 球队阵容关系 | P0/P1 |
| 足球数据域 | `team_honor` | 球队荣誉 | P1 |
| 比赛数据域 | `match_info` | 比赛基础信息 | P0 |
| 比赛数据域 | `match_event` | 比赛事件 | P0 |
| 比赛数据域 | `match_report` | 战报与比赛绑定 | P0 |
| 比赛数据域 | `match_team_stat` | 单场球队技术统计 | P1 |
| 比赛数据域 | `match_player_stat` | 单场球员技术统计 | P1 |
| 比赛数据域 | `match_lineup` | 阵容 | P1/P2 |
| 评分域 | `match_player_rating` | 用户给球员评分 | P1 |
| 评分域 | `match_referee_rating` | 用户给裁判评分 | P1/P2 |
| 榜单域 | `standing_table` | 积分榜/淘汰赛榜单主表 | P1 |
| 榜单域 | `standing_row` | 积分榜行 | P1 |
| 榜单域 | `player_rank` | 球员榜 | P1 |
| 榜单域 | `team_rank` | 球队榜 | P1 |
| 约球域 | `pickup_court` | 球场 | P2 |
| 约球域 | `pickup_activity` | 约球活动 | P2 |
| 约球域 | `pickup_participant` | 约球报名 | P2 |
| 消息域 | `message` | 系统/互动消息 | P0/P1 |
| 推荐域 | `recommend_result` | 推荐结果占位 | P1 |
| 推荐域 | `user_behavior_log` | 用户点击/浏览行为 | P1 |
| 管理后台域 | `admin_operation_log` | 后台操作日志 | P0 |

---

# 6. 用户域

## 6.1 `sys_user`

用途：保存用户账号和登录基础信息。

| 字段 | 类型 | 必须 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 是 | ASSIGN_ID | 用户 ID |
| `username` | `VARCHAR(64)` | 是 | 无 | 用户名，唯一，用于登录或展示占位 |
| `phone` | `VARCHAR(32)` | 否 | `NULL` | 手机号，唯一 |
| `email` | `VARCHAR(128)` | 否 | `NULL` | 邮箱 |
| `password_hash` | `VARCHAR(255)` | 是 | 无 | BCrypt 加密密码 |
| `role_type` | `VARCHAR(32)` | 是 | `USER` | `USER` / `ADMIN` |
| `last_login_time` | `DATETIME` | 否 | `NULL` | 最近登录时间 |
| `onboarding_completed` | `TINYINT` | 是 | `0` | 是否完成首次偏好选择 |
| 通用字段 | - | - | - | `status` / `is_deleted` / `extra_json` / 时间字段 |

索引：

| 索引 | 字段 | 说明 |
|---|---|---|
| `uk_username` | `username` | 登录唯一约束 |
| `uk_phone` | `phone` | 手机号唯一约束 |
| `idx_role_status` | `role_type,status` | 后台筛选 |

说明：`username` 可以作为登录唯一索引，但业务关联必须使用 `user_id`，不能用 username 作为业务关联字段。

## 6.2 `user_profile`

用途：保存用户展示资料、主队、统计信息。

| 字段 | 类型 | 必须 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 是 | ASSIGN_ID | 主键 |
| `user_id` | `BIGINT` | 是 | 无 | 用户 ID |
| `nickname` | `VARCHAR(64)` | 是 | 无 | 昵称 |
| `avatar_url` | `VARCHAR(512)` | 否 | `NULL` | 头像 |
| `bio` | `VARCHAR(512)` | 否 | `NULL` | 简介 |
| `main_team_id` | `BIGINT` | 否 | `NULL` | 我的主队 ID |
| `post_count` | `INT` | 是 | `0` | 发布数 |
| `follower_count` | `INT` | 是 | `0` | 粉丝数 |
| `following_count` | `INT` | 是 | `0` | 关注用户数 |
| `team_follow_count` | `INT` | 是 | `0` | 关注球队数，第一版暂不设置数量上限 |
| `player_follow_count` | `INT` | 是 | `0` | 关注球员数 |
| 通用字段 | - | - | - | `status` / `is_deleted` / `extra_json` / 时间字段 |

索引：`uk_user_id(user_id)`、`idx_main_team_id(main_team_id)`。

## 6.3 `user_onboarding`

用途：记录首次登录选择过程，方便后续重置或补充。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `user_id` | `BIGINT` | 用户 ID |
| `main_team_id` | `BIGINT` | 我的主队 |
| `selected_team_ids` | `JSON` | 关注球队 ID 列表 |
| `selected_player_ids` | `JSON` | 关注球员 ID 列表 |
| `completed` | `TINYINT` | 是否完成 |
| `completed_time` | `DATETIME` | 完成时间 |
| 通用字段 | - | 状态、扩展、时间 |

索引：`uk_user_id(user_id)`。

---

# 7. 内容域

## 7.1 `content`

用途：保存资讯、帖子、文章、战报、讨论内容。

| 字段 | 类型 | 必须 | 默认值 | 说明 |
|---|---|---:|---|---|
| `id` | `BIGINT` | 是 | ASSIGN_ID | 内容 ID |
| `content_type` | `VARCHAR(32)` | 是 | 无 | `NEWS` / `POST` / `ARTICLE` / `REPORT` / `DISCUSS` |
| `content_format` | `VARCHAR(32)` | 是 | `POST_FORMAT` | 帖子形式 / 文章形式 |
| `card_type` | `VARCHAR(32)` | 是 | `CONTENT_CARD` | 首页卡片类型 |
| `title` | `VARCHAR(255)` | 是 | 无 | 标题，卡片最多展示两行 |
| `summary` | `VARCHAR(512)` | 否 | `NULL` | 摘要 |
| `body` | `TEXT` | 否 | `NULL` | 正文，帖子/简单文章可直接使用 |
| `cover_url` | `VARCHAR(512)` | 否 | `NULL` | 封面 |
| `author_id` | `BIGINT` | 是 | 无 | 作者用户 ID |
| `source_type` | `VARCHAR(32)` | 是 | `USER` | `USER` / `ADMIN` / `EXTERNAL` |
| `source_name` | `VARCHAR(128)` | 否 | `NULL` | 来源 |
| `is_official` | `TINYINT` | 是 | `0` | 官方/认证内容占位 |
| `view_count` | `INT` | 是 | `0` | 浏览数 |
| `like_count` | `INT` | 是 | `0` | 点赞数 |
| `comment_count` | `INT` | 是 | `0` | 评论数 |
| `favorite_count` | `INT` | 是 | `0` | 收藏数 |
| `hot_score` | `DECIMAL(12,2)` | 是 | `0` | 热度分 |
| `publish_time` | `DATETIME` | 否 | `NULL` | 发布时间 |
| 通用字段 | - | - | - | `status` / `is_deleted` / `extra_json` / 时间字段 |

索引：

```text
idx_type_status_time(content_type, status, publish_time)
idx_card_status_time(card_type, status, publish_time)
idx_author_status_time(author_id, status, publish_time)
idx_hot_time(status, hot_score, publish_time)
```

## 7.2 `content_block`

用途：文章形式的图文分段。帖子可以不用。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `content_id` | `BIGINT` | 内容 ID |
| `block_type` | `VARCHAR(32)` | `TEXT` / `IMAGE` / `VIDEO_EMBED` |
| `text_content` | `TEXT` | 文本内容 |
| `media_url` | `VARCHAR(512)` | 媒体或外部视频链接 |
| `sort_order` | `INT` | 排序 |
| 通用字段 | - | 状态、扩展、时间 |

索引：`idx_content_sort(content_id, sort_order)`。

## 7.3 `content_media`

用途：保存内容关联图片、视频链接、封面等媒体。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 媒体 ID |
| `content_id` | `BIGINT` | 内容 ID |
| `media_type` | `VARCHAR(32)` | `IMAGE` / `VIDEO_LINK` / `GIF` |
| `media_url` | `VARCHAR(512)` | 媒体地址 |
| `thumbnail_url` | `VARCHAR(512)` | 缩略图 |
| `width` | `INT` | 图片宽度，占位 |
| `height` | `INT` | 图片高度，占位 |
| `sort_order` | `INT` | 排序 |
| 通用字段 | - | 状态、扩展、时间 |

索引：`idx_content_id(content_id)`。

## 7.4 `content_relation`

用途：支持内容关联多个球队、球员、比赛、赛事、热点事件。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `content_id` | `BIGINT` | 内容 ID |
| `relation_type` | `VARCHAR(32)` | `TEAM` / `PLAYER` / `MATCH` / `LEAGUE` / `HOT_EVENT` |
| `relation_id` | `BIGINT` | 关联对象 ID |
| `confidence` | `DECIMAL(5,4)` | 自动识别置信度，手动关联可为 1 |
| `source_type` | `VARCHAR(32)` | `MANUAL` / `AI` / `RULE` |
| 通用字段 | - | 状态、扩展、时间 |

索引：

```text
uk_content_relation(content_id, relation_type, relation_id)
idx_relation(relation_type, relation_id)
```

## 7.5 `hot_event`

用途：热点事件，例如“巴萨 vs 拜仁”“梅西转会皇马”等。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 热点事件 ID |
| `event_name` | `VARCHAR(255)` | 热点名称 |
| `event_type` | `VARCHAR(32)` | `MATCH_HOT` / `TRANSFER` / `TOPIC` |
| `start_time` | `DATETIME` | 开始时间 |
| `end_time` | `DATETIME` | 结束时间 |
| `hot_score` | `DECIMAL(12,2)` | 热度分 |
| 通用字段 | - | 状态、扩展、时间 |

---

# 8. 互动域

## 8.1 `comment`

用途：保存内容评论、比赛评论、评分详情评论和二级回复。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 评论 ID |
| `target_type` | `VARCHAR(32)` | `CONTENT` / `MATCH` / `PLAYER_RATING` |
| `target_id` | `BIGINT` | 评论对象 ID |
| `parent_id` | `BIGINT` | 父评论 ID，根评论为 0 |
| `user_id` | `BIGINT` | 评论用户 |
| `content_text` | `VARCHAR(2000)` | 评论内容 |
| `like_count` | `INT` | 点赞数 |
| `reply_count` | `INT` | 回复数 |
| `hot_score` | `DECIMAL(12,2)` | 热度分 |
| `is_top` | `TINYINT` | 是否置顶 |
| `user_rating_score` | `DECIMAL(3,1)` | 用户评论时附带的评分，可空 |
| 通用字段 | - | 状态、扩展、时间 |

索引：

```text
idx_target_time(target_type, target_id, create_time)
idx_target_hot(target_type, target_id, hot_score)
idx_parent_id(parent_id)
idx_user_time(user_id, create_time)
```

评论排序可实时计算，不一定全部入库：

```text
热度分 = 点赞数 × 1 + 回复数 × 2
最终排序分 = 热度分 × 时间系数
```

## 8.2 `like_record`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 点赞 ID |
| `user_id` | `BIGINT` | 用户 ID |
| `target_type` | `VARCHAR(32)` | `CONTENT` / `COMMENT` / `MATCH` |
| `target_id` | `BIGINT` | 对象 ID |
| `status` | `VARCHAR(32)` | ACTIVE 表示已点赞 |
| `create_time` | `DATETIME` | 创建时间 |
| `update_time` | `DATETIME` | 更新时间 |

索引：`uk_user_target(user_id,target_type,target_id)`。

## 8.3 `favorite_record`

字段与 `like_record` 类似，`target_type` 第一版主要为 `CONTENT`。

索引：`uk_user_target(user_id,target_type,target_id)`。

---

# 9. 关注域

## 9.1 `follow_record`

用途：保存用户关注用户、球队、球员的关系。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 关注 ID |
| `user_id` | `BIGINT` | 发起关注的用户 |
| `follow_type` | `VARCHAR(32)` | `USER` / `TEAM` / `PLAYER` |
| `target_id` | `BIGINT` | 被关注对象 ID |
| `is_main` | `TINYINT` | 是否主队/重点关注 |
| `status` | `VARCHAR(32)` | `ACTIVE` / `CANCELLED` |
| 通用字段 | - | 状态、扩展、时间 |

索引：

```text
uk_user_follow(user_id, follow_type, target_id)
idx_target_follow(follow_type, target_id)
idx_user_type(user_id, follow_type)
```

业务约束：

```text
follow_type=TEAM 暂不设置数量上限。
主队 main_team_id 同步写入 user_profile。
```

粉丝状态通过双向关系计算：

```text
A 关注 B，B 未关注 A：B 看到“回关”
A 关注 B，B 关注 A：互相关注
A 关注 B：A 看到已关注
```

---

# 10. 足球基础数据域

## 10.1 `football_league`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 联赛/赛事 ID |
| `league_name` | `VARCHAR(128)` | 名称 |
| `league_name_en` | `VARCHAR(128)` | 英文名 |
| `country` | `VARCHAR(64)` | 国家/地区 |
| `logo_url` | `VARCHAR(512)` | Logo |
| `season` | `VARCHAR(32)` | 默认赛季 |
| `league_type` | `VARCHAR(32)` | `LEAGUE` / `CUP` |
| `sort_order` | `INT` | 排序 |
| 通用字段 | - | 状态、扩展、时间 |

索引：`idx_type_sort(league_type, sort_order)`。

## 10.2 `football_team`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 球队 ID |
| `team_name` | `VARCHAR(128)` | 球队名称 |
| `team_name_en` | `VARCHAR(128)` | 英文名 |
| `short_name` | `VARCHAR(64)` | 简称 |
| `logo_url` | `VARCHAR(512)` | 队徽 |
| `country` | `VARCHAR(64)` | 国家 |
| `city` | `VARCHAR(64)` | 城市 |
| `home_stadium` | `VARCHAR(128)` | 主场 |
| `founded_year` | `INT` | 成立年份 |
| `coach_name` | `VARCHAR(128)` | 主教练 |
| `market_value` | `VARCHAR(64)` | 总身价展示字段 |
| `follower_count` | `INT` | 关注数 |
| 通用字段 | - | 状态、扩展、时间 |

索引：`idx_team_name(team_name)`。

## 10.3 `football_player`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 球员 ID |
| `player_name` | `VARCHAR(128)` | 球员名 |
| `player_name_en` | `VARCHAR(128)` | 英文名 |
| `avatar_url` | `VARCHAR(512)` | 头像 |
| `nationality` | `VARCHAR(128)` | 国籍，允许多国籍文本 |
| `shirt_number` | `INT` | 球衣号码 |
| `position` | `VARCHAR(32)` | `FW` / `MF` / `DF` / `GK` / `COACH` |
| `birth_date` | `DATE` | 生日 |
| `height_cm` | `INT` | 身高 |
| `weight_kg` | `INT` | 体重 |
| `market_value` | `VARCHAR(64)` | 身价 |
| `retired` | `TINYINT` | 是否退役 |
| `follower_count` | `INT` | 关注数 |
| 通用字段 | - | 状态、扩展、时间 |

## 10.4 `team_player`

用途：球队阵容关系，支持俱乐部和国家队。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `team_id` | `BIGINT` | 球队 ID |
| `player_id` | `BIGINT` | 球员 ID |
| `team_type` | `VARCHAR(32)` | `CLUB` / `NATIONAL` |
| `season` | `VARCHAR(32)` | 赛季 |
| `shirt_number` | `INT` | 号码 |
| `position` | `VARCHAR(32)` | 位置 |
| 通用字段 | - | 状态、扩展、时间 |

索引：`idx_team_season(team_id, season)`、`idx_player(player_id)`。

## 10.5 `team_honor`

字段：`team_id`、`honor_name`、`honor_type`、`winner_count`、`winner_years`、通用字段。

---

# 11. 比赛数据域

## 11.1 `match_info`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 比赛 ID |
| `league_id` | `BIGINT` | 联赛/赛事 ID |
| `season` | `VARCHAR(32)` | 赛季 |
| `round_name` | `VARCHAR(64)` | 轮次 |
| `home_team_id` | `BIGINT` | 主队 |
| `away_team_id` | `BIGINT` | 客队 |
| `home_score` | `INT` | 主队比分 |
| `away_score` | `INT` | 客队比分 |
| `match_time` | `DATETIME` | 比赛时间 |
| `venue` | `VARCHAR(128)` | 场地 |
| `match_status` | `VARCHAR(32)` | 状态 |
| `important_level` | `INT` | 重要程度，用于重要比赛 tab |
| `has_report` | `TINYINT` | 是否有关联战报 |
| 通用字段 | - | 状态、扩展、时间 |

索引：

```text
idx_league_time(league_id, match_time)
idx_home_time(home_team_id, match_time)
idx_away_time(away_team_id, match_time)
idx_status_time(match_status, match_time)
idx_important_time(important_level, match_time)
```

## 11.2 `match_event`

字段：

```text
match_id, team_id, player_id, assist_player_id, event_type, minute, extra_minute, score_after, description, has_debate
```

索引：`idx_match_minute(match_id, minute)`。

## 11.3 `match_report`

字段：`match_id`、`content_id`、`report_type`、`status`、通用字段。  
索引：`uk_match_content(match_id, content_id)`。

## 11.4 `match_team_stat`

用途：单场比赛球队技术统计。

核心字段：

```text
match_id, team_id,
possession_rate, shots, shots_on_target,
big_chances, pass_success_rate,
tackles, interceptions, clearances, saves,
extra_json
```

索引：`uk_match_team(match_id, team_id)`。

## 11.5 `match_player_stat`

用途：单场球员关键数据和评分详情顶部 8 项数据。

核心字段：

```text
match_id, team_id, player_id,
minutes_played, goals, assists,
shots, passes, tackles, interceptions,
media_rating, extra_json
```

索引：`uk_match_player(match_id, player_id)`。

## 11.6 `match_lineup`

用途：阵容 tab。

字段：`match_id`、`team_id`、`player_id`、`lineup_type`、`position_name`、`x_pos`、`y_pos`、`shirt_number`、通用字段。

---

# 12. 评分域

## 12.1 `match_player_rating`

用途：用户对某场比赛某个球员评分。

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `BIGINT` | 主键 |
| `match_id` | `BIGINT` | 比赛 ID |
| `player_id` | `BIGINT` | 球员 ID |
| `team_id` | `BIGINT` | 球队 ID |
| `user_id` | `BIGINT` | 用户 ID |
| `rating_score` | `DECIMAL(3,1)` | 用户评分 |
| 通用字段 | - | 状态、扩展、时间 |

索引：

```text
uk_user_match_player(user_id, match_id, player_id)
idx_match_player(match_id, player_id)
idx_match_team(match_id, team_id)
```

说明：同一用户对同一场比赛同一球员重新评分时覆盖原记录。

## 12.2 `match_rating_summary`

用途：缓存平均评分，减少实时聚合压力。

字段：`match_id`、`target_type`、`target_id`、`rating_count`、`average_score`、`last_rating_time`、通用字段。

`target_type`：`PLAYER` / `REFEREE`。

---

# 13. 榜单域

## 13.1 `standing_table`

字段：`league_id`、`season`、`table_type`、`stage_name`、`display_mode`、通用字段。

说明：`display_mode` 可为 `LEAGUE_TABLE` / `KNOCKOUT_TREE`。

## 13.2 `standing_row`

字段：`table_id`、`team_id`、`rank_no`、`played`、`wins`、`draws`、`losses`、`goals_for`、`goals_against`、`goal_diff`、`points`、通用字段。

索引：`idx_table_rank(table_id, rank_no)`。

## 13.3 `player_rank`

字段：`league_id`、`season`、`rank_type`、`player_id`、`team_id`、`rank_no`、`stat_value`、通用字段。

`rank_type`：`GOALS` / `ASSISTS` / `RATING` / `SAVES`。

## 13.4 `team_rank`

字段：`league_id`、`season`、`rank_type`、`team_id`、`rank_no`、`stat_value`、通用字段。

---

# 14. 约球、消息、推荐、后台日志

## 14.1 `pickup_court` / `pickup_activity` / `pickup_participant`

约球模块 P2 预留。第一轮不强制实现。

`pickup_court` 字段：`court_name`、`city`、`address`、`verified`、`cover_url`、通用字段。  
`pickup_activity` 字段：`creator_id`、`court_id`、`title`、`description`、`start_time`、`end_time`、`max_players`、`joined_count`、`fee_amount`、`activity_status`、通用字段。  
`pickup_participant` 字段：`activity_id`、`user_id`、`join_status`、`remark`、通用字段。

## 14.2 `message`

字段：`receiver_id`、`sender_id`、`message_type`、`title`、`content_text`、`target_type`、`target_id`、`is_read`、通用字段。

第一版消息类型：

```text
SYSTEM / COMMENT / LIKE / FOLLOW / RATING
```

私信暂缓。

## 14.3 `recommend_result`

字段：`user_id`、`scene`、`target_type`、`target_id`、`card_type`、`score`、`reason`、`algorithm_version`、`expire_time`、通用字段。

## 14.4 `user_behavior_log`

字段：`user_id`、`behavior_type`、`target_type`、`target_id`、`scene`、`stay_seconds`、`extra_json`、`create_time`。

用于后续推荐，不作为强实时行为系统。

## 14.5 `admin_operation_log`

字段：`admin_user_id`、`operation_type`、`target_type`、`target_id`、`operation_desc`、`request_ip`、通用字段。

## 14.6 `file_resource`

Purpose: local uploaded image metadata for T09.

Fields:

```text
id
user_id
biz_type
original_name
storage_name
object_key
relative_path
url
content_type
extension
size_bytes
storage_type
bucket
endpoint
public_domain
etag
status
created_at
updated_at
deleted
```

Indexes:

```text
uk_file_object_key(object_key)
idx_file_user_id(user_id)
idx_file_biz_type(biz_type)
idx_file_created_at(created_at)
idx_file_status(status)
idx_file_storage_type(storage_type)
```

Notes:

```text
relative_path is always relative, for example 2026/07/04/uuid.png.
url is public API form, for example /api/public/files/{fileId}.
Local absolute storage paths are never returned to the frontend.
storage_type is LOCAL by default; cloud placeholder values can be stored later for historical files.
```

---

# 15. seed 数据建议

第一版至少准备：

```text
1 个管理员
2 个普通用户
3 个联赛/赛事
8 支球队
40 个球员
10 场比赛
10 篇内容
30 条评论
若干点赞、收藏、关注
2 个热点事件
若干比赛事件
若干榜单行
若干消息
```

建议文件：

```text
scripts/sql/schema.sql
scripts/sql/seed.sql
```

## T11 User Social Schema Notes

T11 does not add tables or columns.

```text
follow_record.follow_type=USER stores user-to-user follows.
follow_record.user_id is the follower.
follow_record.target_id is the followed user.
status=ACTIVE means currently following.
status=CANCELLED means previously followed and now unfollowed.
uk_user_follow(user_id, follow_type, target_id) keeps follow/unfollow idempotent.
idx_target_follow(follow_type, target_id) supports follower lists.
idx_user_type(user_id, follow_type) supports following lists.
```

Counts:

```text
user_profile.following_count is synced from ACTIVE USER follows by the current user.
user_profile.follower_count is synced from ACTIVE USER follows targeting that user.
No hard cap is applied to USER, TEAM, or PLAYER follows.
```

## T12 Comment Hot Schema Notes

T12 reuses `comment` and `like_record`.

Added comment columns:

```text
root_id BIGINT NULL
reply_to_user_id BIGINT NULL
idx_root_id(root_id)
```

Meaning:

```text
parent_id is the directly replied comment, or 0 for root comments.
root_id is the root comment id. For root comments, root_id equals id.
reply_to_user_id is optional and used for "reply to user" display.
like_record stores comment likes with target_type=COMMENT and target_id=comment.id.
```

目标：

```text
后端启动后，App 首页、数据页、球队详情、球员详情、比赛详情、我的页面、后台列表都有数据可展示。
```
