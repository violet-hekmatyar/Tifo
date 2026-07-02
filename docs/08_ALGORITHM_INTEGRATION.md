# 南看台算法推荐预留接口

> 版本：v0.2-tifo-revised  
> 定位：定义南看台第一版为算法同学预留的推荐模块边界、输入输出、接口形式、降级策略和数据表占位。本文不实现算法模型。

## 1. 算法模块定位

算法部分围绕：

```text
首页卡片推荐
资讯 tab 排序
关注 tab 兜底
我关注的球队内容流
球队推荐
球员推荐
比赛推荐
热门评论排序
数据辩论话题推荐
```

第一版后端先实现简单规则，后期算法同学接入模型服务。

## 2. 推荐模块边界

后端负责：

```text
提供推荐接口
准备用户画像基础数据
准备内容、球队、球员、比赛、卡片候选数据
调用算法服务
处理超时和异常
提供降级推荐
记录推荐结果
记录用户行为日志
```

算法服务负责：

```text
根据输入特征返回推荐列表
给出推荐分数
可选给出推荐原因
```

算法服务不负责登录、权限、数据库写入、内容详情查询、足球数据维护。

## 3. 推荐核心原则

### 3.1 权重优先级

```text
主队 > 关注球队 > 关注球员 > 关注用户 > 热点赛事 > 普通热度
```

### 3.2 首页推荐特点

首页推荐不是单一内容列表，而是多卡片混排：

```text
资讯卡片
比赛卡片
榜单卡片
评分卡片
讨论卡片
热门评论卡片
转会卡片
```

推荐必须保证资讯卡片占主要比例，同时允许插入比赛、评分、榜单等卡片。

### 3.3 实时性要求

足球 APP 的推荐相比普通小红书更强调实时性：

```text
赛前：关注球队或重要比赛卡片应提前出现
赛中：比赛卡片可根据关注情况靠前
赛后：赛果、战报、评分卡片应及时出现
热点期：世界杯、欧洲杯、欧冠淘汰赛等热点赛事内容提高权重
```

第一版不做 WebSocket，但需要通过刷新时排序保证重点内容靠前。

## 4. 第一版规则推荐

基础推荐分：

```text
推荐分 = 关注权重 + 内容热度 + 时间衰减 + 热点赛事权重 + 卡片类型权重 - 相似内容惩罚
```

示例权重：

| 因素 | 权重 |
|---|---:|
| 内容关联主队 | +100 |
| 内容关联关注球队 | +60 |
| 内容关联关注球员 | +40 |
| 内容来自关注用户 | +50 |
| 重要比赛赛前/赛中/赛后 | +80 |
| 内容点赞数 | like_count * 2 |
| 内容评论数 | comment_count * 3 |
| 内容收藏数 | favorite_count * 4 |
| 新内容保护 | 0~20 |
| 相似内容惩罚 | -10 ~ -50 |

## 5. 推荐场景

| scene | 说明 |
|---|---|
| `HOME_RECOMMEND` | 首页推荐，多卡片混排 |
| `NEWS_ONLY` | 资讯 tab，只展示资讯文章卡片 |
| `FOLLOWING_FEED` | 关注用户内容流 |
| `TEAM_FOLLOW_FEED` | 我关注的球队内容流 |
| `MATCH_IMPORTANT` | 重要比赛推荐 |
| `POST_MATCH_RATING` | 赛后评分卡片推荐 |
| `TEAM_RECOMMEND` | 推荐球队 |
| `PLAYER_RECOMMEND` | 推荐球员 |
| `HOT_COMMENT` | 热评排序 |
| `DEBATE_TOPIC` | 数据辩论话题 |

## 6. 卡片混排规则

第一版建议：

```text
资讯卡片比例保持最高；
比赛卡片在赛前/赛中/赛后可提升；
评分卡片只在比赛结束后出现；
榜单卡片低频穿插；
讨论卡片根据热度穿插；
转会卡片 P2 预留。
```

示例比例，仅用于第一版参考：

| 卡片类型 | 推荐 tab 比例 |
|---|---:|
| 资讯/帖子卡片 | 60%~75% |
| 比赛卡片 | 10%~20% |
| 热门评论/讨论卡片 | 5%~10% |
| 排名/评分卡片 | 5%~10% |
| 转会/广告卡片 | P2 |

## 7. 冷启动与关注页兜底

### 7.1 新用户冷启动

依赖首次登录选择：

```text
我的主队
关注球队
关注球员
```

如果用户没有足够行为数据，按这些偏好推荐。

### 7.2 关注 tab 兜底

当用户未关注任何博主，或者关注用户内容太少：

```text
穿插近期高热度用户内容
推荐可能感兴趣的博主
不展示空白页
```

## 8. 用户行为记录

建议记录：

```text
曝光
点击
进入详情
点赞
收藏
评论
关注
停留时长
评分
```

用于后续推荐，不要求第一版全部进入复杂算法。

对应表：

```text
user_behavior_log
```

## 9. 后期算法服务接入方式

推荐采用 HTTP 服务：

```text
south-stand-server
-> recommendation-service
```

算法服务可由 Python 实现，后端通过 `RecommendClient` 调用。

内部接口预留：

```text
POST /api/internal/recommend/feed
POST /api/internal/recommend/cards
POST /api/internal/recommend/teams
POST /api/internal/recommend/players
POST /api/internal/recommend/matches
```

这些是内部接口，不直接暴露给 App 前端。

## 10. 推荐输入结构

```json
{
  "userId": 10002,
  "scene": "HOME_RECOMMEND",
  "pageNum": 1,
  "pageSize": 20,
  "userProfile": {
    "mainTeamId": 30001,
    "followTeamIds": [30001, 30002],
    "followPlayerIds": [40001],
    "followUserIds": [10003],
    "recentClickedTargetIds": [20001],
    "recentLikedContentIds": [20002]
  },
  "candidateItems": [
    {
      "cardType": "CONTENT_CARD",
      "targetType": "CONTENT",
      "targetId": 20001,
      "contentType": "REPORT",
      "teamIds": [30001],
      "playerIds": [40001],
      "matchId": 50001,
      "likeCount": 88,
      "commentCount": 24,
      "favoriteCount": 30,
      "publishTime": "2026-06-23 09:00:00"
    }
  ]
}
```

## 11. 推荐输出结构

```json
{
  "success": true,
  "scene": "HOME_RECOMMEND",
  "algorithmVersion": "RULE_V1",
  "items": [
    {
      "cardType": "CONTENT_CARD",
      "targetType": "CONTENT",
      "targetId": 20001,
      "score": 0.9123,
      "reason": "你关注了巴塞罗那，且该内容热度较高。"
    }
  ],
  "fallback": false,
  "errorCode": null,
  "errorMessage": null
}
```

## 12. App 推荐接口

App 前端不直接调用算法服务，而是调用后端：

```http
GET /api/app/feed?tab=recommend&pageNum=1&pageSize=10
```

后端流程：

```text
获取当前用户
-> 查询候选卡片
-> 调用 RecommendService
-> 推荐结果排序
-> 拼装 CardVO
-> 返回给前端
```

## 13. 降级策略

算法服务不可用时必须降级。

触发条件：

```text
算法服务超时
算法服务返回失败
算法服务返回空列表
算法服务不可连接
```

降级规则：

```text
主队内容优先
-> 关注球队内容
-> 关注球员内容
-> 重要比赛
-> 热度排序
-> 发布时间倒序
```

## 14. 超时设置

| 调用 | 超时 |
|---|---:|
| 首页推荐 | 800ms |
| 卡片混排 | 1000ms |
| 球队/球员推荐 | 1000ms |
| 后台批量生成推荐 | 3000ms |

第一版推荐同步调用，后期可以异步预计算。

## 15. 推荐结果表

对应数据库：

```text
recommend_result
user_behavior_log
```

用途：

```text
缓存推荐结果
记录算法版本
辅助排查推荐效果
给前端占位数据
支持后续行为学习
```

## 16. 算法同学交付物

算法同学后期至少需要提供：

```text
推荐服务启动方式
HTTP 接口文档
输入 JSON 样例
输出 JSON 样例
算法版本号
超时和异常说明
最小测试样例
推荐效果说明
```

## 17. 后端预留代码结构

```text
recommend
├── RecommendService
├── RuleRecommendServiceImpl
├── RemoteRecommendClient
├── CardMixService
├── dto
│   ├── RecommendRequest
│   └── RecommendCandidate
└── vo
    ├── RecommendResult
    └── RecommendItem
```

## 18. 当前不做

```text
实时特征流
用户向量
内容向量
Embedding
召回粗排精排全链路
复杂 A/B 测试
推荐效果指标平台
深度学习模型在线推理
```
