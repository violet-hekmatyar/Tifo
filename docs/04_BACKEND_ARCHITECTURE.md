# 南看台后端架构与模块划分

> 版本：v0.2-tifo-revised  
> 定位：定义南看台第一版后端架构、包结构、模块边界和调用关系。本文不负责数据库字段、接口路径和部署命令。

## 1. 总体架构

第一版采用：

```text
App / 管理后台
        ↓
Nginx 可选
        ↓
south-stand-server
        ↓
MySQL + Redis + 文件存储
```

可选保留：

```text
Spring Cloud Gateway
Nacos
```

但第一版业务服务不拆成多个微服务，先采用单体模块化。

## 2. 为什么先单体模块化

原因：

1. 当前主要开发者只有一人；
2. 业务仍在快速变化；
3. 微服务会增加部署、调试和接口调用成本；
4. Docker 部署阶段更适合先运行单个业务镜像；
5. 包内模块化已经足够支撑第一版。

后期可拆分：

```text
user-service
content-service
football-service
interaction-service
message-service
recommendation-service
admin-service
```

## 3. 推荐包结构

```text
com.southstand
├── SouthStandApplication.java
├── common
│   ├── result
│   ├── exception
│   ├── constant
│   ├── enums
│   ├── utils
│   └── config
├── auth
├── user
├── onboarding
├── content
├── card
├── interaction
├── follow
├── football
│   ├── league
│   ├── team
│   ├── player
│   ├── match
│   ├── schedule
│   ├── ranking
│   ├── stat
│   ├── event
│   └── rating
├── topic
├── message
├── pickup
├── admin
├── recommend
└── infrastructure
    ├── file
    ├── cache
    └── client
```

每个业务模块建议分层：

```text
controller  接口入口
service     业务逻辑
mapper      数据访问
entity      数据库实体
dto         请求参数
vo          响应对象
enums       模块内枚举
```

## 4. 模块职责

### 4.1 `common`

负责统一返回、分页、全局异常、错误码、通用枚举、工具类和基础配置。不写业务逻辑。

### 4.2 `auth`

负责注册、登录、JWT 生成与校验、当前用户上下文、密码加密、管理端鉴权。

不负责：

```text
用户主页
复杂 RBAC
多端登录
私信权限
```

### 4.3 `onboarding`

负责首次登录个性选择：

```text
我的主队
关注球队
关注球员
是否完成首次设置
```

它会调用 `user`、`follow`、`football` 模块，但不直接操作其他模块 Mapper。

### 4.4 `user`

负责用户基础信息、用户资料、个人主页统计、用户状态、头像简介、主队设置。

注意：`我的页面` 不是单独模块，而是 `user` 聚合 `content`、`follow`、`interaction`、`football` 等模块数据。

### 4.5 `content`

负责资讯、帖子、文章、战报、讨论内容、内容媒体、内容状态、后台发布内容、用户发布内容。

内容类型建议：

```text
NEWS       资讯
POST       帖子
ARTICLE    文章
REPORT     战报
DISCUSS    讨论
```

内容展示形式建议：

```text
POST_FORMAT      帖子形式，文字和图片集中展示
ARTICLE_FORMAT   文章形式，图文分段展示
```

内容可绑定：

```text
球队
球员
比赛
赛事
热点事件
```

### 4.6 `card`

负责首页卡片协议和卡片聚合。

卡片类型：

```text
CONTENT_CARD
MATCH_CARD
RANKING_CARD
RATING_CARD
DISCUSSION_CARD
HOT_COMMENT_CARD
TRANSFER_CARD
AD_CARD
```

第一版至少支持：

```text
CONTENT_CARD
MATCH_CARD
```

后续新增卡片时，不应破坏已有首页接口结构。

### 4.7 `interaction`

负责评论、二级回复、点赞、收藏、浏览记录、热评排序。

评论排序由以下数据支撑：

```text
点赞数
回复数
发布时间
置顶标识
```

点赞和收藏使用：

```text
target_type + target_id
```

以支持内容、评论、比赛、球员评分等对象。

### 4.8 `follow`

负责关注用户、关注球队、关注球员、粉丝列表、关注列表、主队关系。

统一设计：

```text
follow_type = USER / TEAM / PLAYER
target_id = 对应对象 ID
```

业务约束：

```text
当前第一版不设置关注球队数量上限。
```

粉丝关系状态通过双向 follow 关系计算：

```text
回关
已关注
互相关注
```

### 4.9 `football`

负责联赛、球队、球员、比赛、比赛事件、球队数据、球员数据、积分榜、球员榜、球队榜。

建议内部拆子域：

| 子域 | 职责 |
|---|---|
| `league` | 赛事/联赛基础信息、赛季 |
| `team` | 球队详情、球队阵容、球队荣誉 |
| `player` | 球员详情、俱乐部/国家队、生涯数据 |
| `match` | 比赛详情、比分、状态 |
| `schedule` | 赛程列表、关注球队赛程、重要比赛 |
| `ranking` | 积分榜、球员榜、球队榜、淘汰赛树 |
| `stat` | 比赛统计、球队赛季数据、球员赛季数据 |
| `event` | 进球、黄牌、红牌、换人、VAR、点球 |
| `rating` | 球员评分、裁判评分、平均分计算 |

第一版数据来源：

```text
后台手动录入
seed.sql 初始化样例数据
```

### 4.10 `topic`

负责热点事件和数据辩论场预留。

支持：

```text
热点事件
讨论主题
比赛事件投票
球员表现讨论
内容关联话题
```

第一版可只做表和接口占位，P1 再完善。

### 4.11 `message`

负责系统消息、评论回复提醒、点赞收藏提醒、关注提醒、后台通知。

第一版不做完整私信聊天。私信相关界面可先作为待确认需求记录。

### 4.12 `pickup`

负责约球活动列表、约球详情、报名参加、取消报名、球场、聊天室等。

当前定位：

```text
P2 预留模块，不作为第一轮主线。
```

第一轮可以只保留包结构和数据库预留，不强制开发完整接口。

### 4.13 `recommend`

负责首页推荐内容、热门内容排序、球队推荐、球员推荐、比赛推荐、算法服务接入预留。

第一版规则排序：

```text
主队权重 > 关注球队权重 > 关注球员权重
+ 内容热度
+ 发布时间衰减
+ 热点比赛/赛事权重
```

后期调用算法服务。

### 4.14 `admin`

负责后台用户管理、内容管理、评论管理、联赛管理、球队管理、球员管理、比赛管理、榜单管理、基础统计。

注意：`admin controller` 可以调用各业务 service，不要重复实现一套业务逻辑。

### 4.15 `infrastructure`

负责文件存储、Redis 缓存、第三方 HTTP Client、第三方赛事 API Client、对象存储适配。不写业务逻辑。

## 5. 页面与后端模块映射

| 原型页面 | 后端主模块 | 说明 |
|---|---|---|
| 首次登录个性选择 | onboarding / follow / football | 设置主队、关注球队、关注球员 |
| 首页 | card / recommend / content / football | 多卡片聚合 |
| 资讯详情 | content / interaction | 内容详情、评论、点赞、收藏 |
| 数据页 | football.schedule / ranking | 赛程、重要比赛、榜单 |
| 球队详情 | football.team / content / follow | 总览、帖子、球员、数据、赛程 |
| 球员详情 | football.player / content / follow | 总览、帖子、数据、比赛、生涯 |
| 比赛详情 | football.match / event / rating / stat | 评分、总览、阵容、排名、统计 |
| 我的 | user / content / follow / interaction | 看台、发布、点赞、收藏、评论 |
| 粉丝关注 | follow / user | 已关注、互相关注、回关 |
| 消息 | message | 系统/互动消息，私信待确认 |
| 约球 | pickup | P2 预留 |
| 管理后台 | admin + 各业务 service | 后台管理 |

## 6. 模块间调用原则

1. Controller 只调用本模块 Service 或聚合 Service。
2. Service 可以调用其他模块 Service，但不要调用其他模块 Mapper。
3. 跨模块数据读取优先通过 Service 方法。
4. 不允许循环依赖。
5. 聚合查询放在当前业务入口模块，例如我的页面放在 `user` 聚合，首页放在 `card/recommend` 聚合。
6. 不直接返回 Entity 给前端。

示例：

```text
UserController.getMyProfile()
-> UserService.getMyProfile()
-> FollowService.countFollow()
-> ContentService.countUserContent()
-> InteractionService.countUserLikes()
```

## 7. 禁止事项

```text
Controller 直接写 SQL
Controller 写复杂业务逻辑
前端直接返回 Entity
Service 直接拼接不受控 SQL
Mapper 返回给前端
业务模块直接访问其他模块 Mapper
AI Coding 自行新增复杂中间件
```
