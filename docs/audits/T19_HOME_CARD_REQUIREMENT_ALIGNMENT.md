# T19 首页卡片需求对齐审计

## 审计范围与结论

本审计依据原始《tifo需求文档》首页相关章节，并对照 T12、T15、T17、T18 现有后端实现。原始需求明确包含资讯、热门讨论、热门评论、比赛、赛后球员评分和排名/榜单：首页以内容为主体，主队及关注对象参与个性化；未规定固定卡片顺序。转会属于选做/后续能力，复杂杯赛树未定，本轮均不实现。

T19 不创建新业务体系，而是在 T18 CONTENT/MATCH 核心推荐完成后，以 Java 规则稀疏插入复用既有数据的辅助卡。

## 对齐表

| 原始需求 | 后端实现 | 数据来源 / 复用能力 | 状态 | 备注 |
| --- | --- | --- | --- | --- |
| 资讯、新闻、帖子、文章 | `CONTENT` | 现有 `content`、T13 发布、T18 RULE_V2/CF_V1 | 完成 | 仍是首页主体；旧 JSON 字段兼容 |
| 比赛 | `MATCH` | `match_info` 及 T14 批量装载、T18 RULE_V2 | 完成 | 保留 core stream 约 7:3 的 MATCH 曝光 |
| 热门讨论 | `DISCUSSION` | 现有 POST、内容关系与互动统计 | 完成 | 评论高权重、时间衰减、主队/关注加权；无 discussion 表 |
| 热门评论 | `HOT_COMMENT` | T12 `CommentService.calculateHotScore` | 完成 | 父内容和作者批量校验；无 hot-comment 表 |
| 积分榜 | `RANKING/STANDING/POINTS` | T15 `football_standing` | 完成 | 当前赛季 Top 5 |
| 球员榜 | `RANKING/PLAYER/GOALS` | T15 `football_player_competition_stat` | 完成 | Top 5；payload 可扩展其他 T15 rankType |
| 球队榜 | `RANKING/TEAM/GOALS_FOR` | T15 `football_team_competition_stat` | 完成 | Top 5；payload 可扩展其他 T15 rankType |
| 赛后球员评分 | `PLAYER_RATING` | T17 `football_match_player_stat`、`football_user_player_rating` | 完成 | 仅 FINISHED；无用户评分时只展示官方评分，不伪造球迷评分 |
| 主队/关注个性化 | 讨论、榜单、赛后评分候选优先级 | `user_profile.main_team_id`、`follow_record` | 完成 | 主队优先于关注球队，稳定规则选择 |
| 首页内容主体 | core 先完成 T18 7:3，再插辅助卡 | T18 `RecommendationService` + T19 composition | 完成 | 辅助卡最终占比不超过 20%，间隔至少 3 张核心卡 |
| 固定卡片顺序 | 未实现固定业务顺序 | 原需求未规定 | 符合 | 使用稳定候选顺序和稳定 `cardKey`，不使用随机数 |
| 首页搜索入口 | 复用 `/api/app/search/entities` | T13 搜索 | 部分具备 | 已支持 TEAM/PLAYER/MATCH；未支持 CONTENT 搜索，不阻塞首页 Feed 第一版 |
| 帖子/文章发布入口 | 复用 `/api/app/contents/posts`、`/articles` | T13 内容发布 | 已具备 | 发布后进入现有内容数据源，可被 Feed 候选读取 |
| 转会中心/转会卡 | 未实现 | 原需求选做 | 延期 | 明确不在 T19 范围 |
| 数据辩论场 | 未实现 | 原需求未确认 | 不做 | DISCUSSION 仅复用普通帖子和互动数据 |
| 复杂杯赛树、事件投票、视频集锦、裁判评分 | 未实现 | 非 T19 核心 | 延期 | 不阻塞首页第一版核心卡片 |

## 编码前数据能力审计

| cardType | 已有底层数据 | 已有独立接口 | T19 前已有 Feed 卡片 | T19 前缺口 |
| --- | --- | --- | --- | --- |
| CONTENT | 是 | 是 | 是 | 无 |
| MATCH | 是 | 是 | 是 | 无 |
| DISCUSSION | 是 | 内容/评论接口可复用 | 否 | 缺 Feed 选择和组合 |
| HOT_COMMENT | 是 | 是（T12） | 仅作为内容附属热评 | 缺独立 Feed 卡片 |
| RANKING | 是 | 是（T15） | 否 | 缺 Feed 聚合 |
| PLAYER_RATING | 是 | 是（T17） | 否 | 缺 Feed 聚合 |

## 前端剩余工作

Flutter 仍需按 `cardType + payload` 实现 DISCUSSION、HOT_COMMENT、RANKING、PLAYER_RATING 四类组件及详情跳转，并补齐埋点映射。CONTENT/MATCH 旧渲染字段保持兼容。若产品第一版要求搜索内容，需另行增加 CONTENT 搜索能力；该项不是 T19 首页 Feed 的后端阻塞项。

