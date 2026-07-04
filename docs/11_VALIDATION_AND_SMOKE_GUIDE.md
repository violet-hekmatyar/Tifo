# 南看台测试与验证计划

> 版本：v0.2-tifo-revised  
> 定位：定义南看台后端第一版的单元测试、接口测试、核心场景测试、Docker 部署验证和 smoke 验收方式。本文是测试计划，不代表所有测试已经完成。

## 1. 验收原则

第一版验收重点：

```text
能启动
能连接数据库
能登录
能完成首次登录偏好设置
能访问核心接口
能完成核心业务闭环
能打包 Docker 镜像
能在 Linux 服务器运行
```

不要求：

```text
高并发压测
完整线上监控
完整自动化测试覆盖率
复杂推荐评测
实时比分压力测试
完整私信聊天测试
完整约球聊天室测试
```

## 2. 测试层级

| 测试类型 | 目标 |
|---|---|
| 单元测试 | 校验 Service 核心逻辑、排序规则、权限和业务约束 |
| 接口测试 | 校验 Controller 入参、鉴权、返回结构、错误码 |
| 场景测试 | 模拟真实用户路径，确认业务闭环可跑通 |
| 部署测试 | 确认 Docker 镜像和 Linux Compose 可运行 |
| 回归测试 | 每轮 AI Coding 后确认核心功能未破坏 |

## 3. 本地基础检查

```powershell
java -version
mvn -version
docker version
docker compose version
```

期望：JDK 17、Maven 3.9.x、Docker 可用。

### 3.1 T00 仓库基础检查

T00 是仓库基础准备任务，当前不创建 Maven / Spring Boot 项目，因此本轮不要求执行：

```text
mvn clean test
mvn clean package
```

T00 必须执行仓库检查脚本：

```powershell
cd D:\Football-APP
powershell -ExecutionPolicy Bypass -File .\scripts\windows\check-repo.ps1
```

通过标准：

```text
当前目录是 Git 仓库
当前分支是 main
origin 指向 https://github.com/violet-hekmatyar/Tifo.git
docs 和 tmp 目录存在
.gitignore 正确忽略 tmp/、.env、target/、logs/、uploads/
README.md 存在
部署文档包含 jar 直跑或 java -jar 说明
Codex 大任务计划包含 T00
.env 未被 Git 跟踪
tmp 下文件未被 Git 跟踪
未创建 pom.xml、src/** 或业务代码
```

### 3.2 T01 Spring Boot 骨架检查

T01 创建最小 Spring Boot 后端骨架，但不连接 MySQL / Redis，不创建数据库脚本，不实现登录或业务接口。

必须执行：

```powershell
cd D:\Football-APP
.\scripts\windows\check-t01.ps1
```

脚本覆盖：

```text
pom.xml 存在
SouthStandApplication 存在
不存在 application-prod.yml、Dockerfile、docker-compose.yml
不存在 schema.sql / seed.sql
mvn clean test 通过
mvn clean package 通过
target/south-stand-server.jar 存在
java -jar 启动成功
GET /api/public/health 返回 code=0、message=success、data.status=UP
```

T01 不要求：

```text
DB health
Redis health
登录鉴权
业务接口
Docker 构建
```

### 3.3 T02 通用基础设施与 MySQL / Redis 检查

T02 接入通用返回、分页、错误码、业务异常、全局异常处理、traceId、MySQL、Redis 和基础 SQL。

必须执行：

```powershell
cd D:\Football-APP
.\scripts\windows\check-t02.ps1
```

脚本覆盖：

```text
.\scripts\windows\reset-dev-db.ps1
mvn clean test
mvn clean package
java -jar target\south-stand-server.jar
GET /api/public/health
GET /api/public/health/db
GET /api/public/health/redis
```

通过标准：

```text
reset-dev-db.ps1 成功
schema.sql / seed.sql 可执行
mvn clean test 成功
mvn clean package 成功
target/south-stand-server.jar 存在
三个 health 接口均返回 code=0
DB / Redis health 的 data.status=UP
```

### 3.4 T03 登录鉴权与用户闭环检查

T03 实现注册、登录、JWT、当前用户、管理员权限占位和登录失败防刷。

必须执行：

```powershell
cd D:\Football-APP
.\scripts\windows\check-t03.ps1
```

脚本覆盖：

```text
.\scripts\windows\reset-dev-db.ps1
mvn clean test
mvn clean package
java -jar target\south-stand-server.jar
.\scripts\windows\smoke-auth.ps1
```

通过标准：

```text
注册唯一测试用户成功
登录返回 accessToken
GET /api/auth/me 带 token 成功
GET /api/auth/me 不带 token 返回 40101
普通 USER 访问 /api/admin/health 返回 40301
seed 管理员访问 /api/admin/health 成功
连续错误密码触发 40103
T03 check passed
```

### 3.5 T04 首次登录与关注闭环检查

T04 实现首次登录推荐选项、保存偏好、关注 toggle 和当前用户资料。

必须执行：

```powershell
cd D:\Football-APP
.\scripts\windows\check-t04.ps1
```

脚本覆盖：

```text
.\scripts\windows\reset-dev-db.ps1
mvn clean test
mvn clean package
java -jar target\south-stand-server.jar
.\scripts\windows\smoke-auth.ps1
.\scripts\windows\smoke-onboarding.ps1
```

通过标准：

```text
T03 smoke-auth.ps1 仍通过
GET /api/app/onboarding/options 返回球队和球员选项
POST /api/app/onboarding/preferences 保存主队、关注球队和关注球员
GET /api/app/users/me/profile 返回 onboardingCompleted=true
POST /api/app/follows/toggle 支持 TEAM 取消关注和重新关注
第 6 支球队关注成功，当前不设置关注球队数量上限
无 Token 访问偏好保存或 profile 返回 40101
T04 check passed
```

## 4. 后端编译检查

```powershell
cd D:\south-stand-dev\south-stand-backend

mvn clean test
mvn clean package
```

通过标准：

```text
BUILD SUCCESS
```

## 5. 单元测试计划

### 5.1 登录与安全

| 测试项 | 标准 |
|---|---|
| BCrypt 加密 | 数据库不出现明文密码 |
| 登录成功 | 正确账号密码返回 JWT |
| 登录失败 | 错误密码返回错误码 |
| 禁用用户 | 禁用后不能登录 |
| JWT 解析 | 能解析 userId、roleType |
| 管理员权限 | 普通用户不能访问后台 |

### 5.2 首次登录偏好

| 测试项 | 标准 |
|---|---|
| 设置主队 | `user_profile.main_team_id` 正确保存 |
| 关注球队 | 生成 follow_record |
| 关注球员 | 生成 follow_record |
| 重复选择 | 不生成重复关注 |
| 关注球队数量 | 当前不设置数量上限，第 6 支球队仍可关注成功 |
| 完成状态 | `onboarding_completed` 更新为 true |

### 5.3 评论排序

| 测试项 | 标准 |
|---|---|
| 热度分 | 点赞数 × 1 + 回复数 × 2 |
| 时间衰减 | 不同时间段使用不同系数 |
| 热评排序 | 按最终排序分降序 |
| 二级回复 | parent_id 关系正确 |

### 5.4 关注关系

| 测试项 | 标准 |
|---|---|
| 关注用户 | 生成 USER 类型关注 |
| 关注球队 | 生成 TEAM 类型关注 |
| 关注球员 | 生成 PLAYER 类型关注 |
| 回关状态 | 对方关注我时显示回关 |
| 互相关注 | 双向关注时显示互相关注 |
| 取消关注 | 状态变更或逻辑删除 |

### 5.5 球员评分

| 测试项 | 标准 |
|---|---|
| 首次评分 | 新增评分记录 |
| 重复评分 | 覆盖原评分 |
| 平均分 | 重新计算平均分 |
| 评分范围 | 非法评分被拒绝 |
| 评论不强制评分 | 未评分也能评论 |

## 6. 接口测试计划

### 6.1 健康检查

```http
GET /api/public/health
GET /api/public/health/db
GET /api/public/health/redis
```

### 6.2 登录 Smoke

```powershell
curl -X POST http://localhost:8080/api/auth/register `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"test_user\",\"phone\":\"13900000001\",\"password\":\"123456\"}"

curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"test_user\",\"password\":\"123456\"}"
```

通过标准：返回 `accessToken` 和当前用户信息。

### 6.3 首次登录偏好 Smoke

```http
POST /api/app/onboarding/preferences
```

测试流程：

```text
注册用户
-> 登录
-> 选择主队
-> 关注 2 支球队
-> 关注 2 名球员
-> 查询当前用户
-> onboardingCompleted=true
```

### 6.4 首页卡片 Smoke

```http
GET /api/app/feed?tab=recommend&pageNum=1&pageSize=10
GET /api/app/feed?tab=news&pageNum=1&pageSize=10
GET /api/app/feed?tab=following&pageNum=1&pageSize=10
GET /api/app/feed?tab=team&teamId=30001&pageNum=1&pageSize=10
```

通过标准：

```text
recommend 返回多类型卡片
news 只返回资讯/文章类卡片
following 无关注内容时不返回空白页，应有兜底内容
team 返回关联球队内容
```

### 6.5 内容与互动 Smoke

```http
GET  /api/app/contents/{contentId}
GET  /api/app/comments?targetType=CONTENT&targetId=20001
POST /api/app/comments
POST /api/app/likes/toggle
POST /api/app/favorites/toggle
```

通过标准：评论、点赞、收藏后，对应计数和状态正确变化。

### 6.6 足球数据 Smoke

```http
GET /api/app/football/leagues
GET /api/app/football/matches/important
GET /api/app/football/matches/following-teams
GET /api/app/football/standings
GET /api/app/football/player-ranks
GET /api/app/football/team-ranks
```

### 6.7 球队详情 Smoke

```http
GET /api/app/football/teams/{teamId}
GET /api/app/football/teams/{teamId}/overview
GET /api/app/football/teams/{teamId}/contents
GET /api/app/football/teams/{teamId}/players
GET /api/app/football/teams/{teamId}/stats
GET /api/app/football/teams/{teamId}/matches
```

### 6.8 球员详情 Smoke

```http
GET /api/app/football/players/{playerId}
GET /api/app/football/players/{playerId}/overview
GET /api/app/football/players/{playerId}/contents
GET /api/app/football/players/{playerId}/stats
GET /api/app/football/players/{playerId}/matches
GET /api/app/football/players/{playerId}/career
```

### 6.9 比赛详情 Smoke

```http
GET /api/app/football/matches/{matchId}
GET /api/app/football/matches/{matchId}/overview
GET /api/app/football/matches/{matchId}/ratings
GET /api/app/football/matches/{matchId}/lineup
GET /api/app/football/matches/{matchId}/ranking
GET /api/app/football/matches/{matchId}/stats
```

### 6.10 球员评分 Smoke

```http
POST /api/app/football/matches/{matchId}/players/{playerId}/rating
GET  /api/app/football/matches/{matchId}/players/{playerId}/rating-detail
```

通过标准：同一用户重复评分覆盖原评分，平均分更新。

### 6.11 我的页面 Smoke

```http
GET /api/app/users/me/profile
GET /api/app/users/me/stand
GET /api/app/users/me/contents?type=post
GET /api/app/users/me/likes
GET /api/app/users/me/favorites
GET /api/app/users/me/comments
```

### 6.12 管理后台 Smoke

需要管理员 token。

```http
GET  /api/admin/contents
POST /api/admin/contents
GET  /api/admin/football/teams
POST /api/admin/football/teams
GET  /api/admin/football/players
POST /api/admin/football/players
GET  /api/admin/football/matches
POST /api/admin/football/matches
```

普通用户访问后台应返回 `40301`。

## 7. 核心场景测试

### 7.1 第一版核心演示场景

```text
管理员登录后台
-> 录入联赛、球队、球员、比赛
-> 发布战报和帖子
-> 普通用户注册登录
-> 首次登录选择主队、关注球队、关注球员
-> 首页看到相关卡片
-> 进入战报详情
-> 评论、回复、点赞、收藏
-> 进入球队详情
-> 进入球员详情
-> 进入比赛详情
-> 进入我的页面查看记录
```

### 7.2 首次登录推荐影响场景

```text
用户 A 选择巴萨为主队
-> 首页推荐优先出现巴萨内容
-> 数据页关注球队赛程出现巴萨赛程
-> 我的看台显示巴萨
```

### 7.3 评论热评排序场景

```text
评论 1：点赞多但发布时间超过 24 小时
评论 2：点赞少但发布时间 1 小时内
评论 3：点赞和回复都高
-> 验证排序符合热度 + 时间衰减规则
```

### 7.4 关注球队数量场景

```text
用户已关注多支球队
-> 继续关注更多球队
-> 关注成功
-> 当前第一版不设置关注球队数量上限
```

### 7.5 粉丝关注状态场景

```text
A 关注 B
-> B 的粉丝列表显示 A 为“回关”
B 回关 A
-> 双方显示“互相关注”
A 取消关注 B
-> B 对 A 状态变化
```

### 7.6 比赛评分场景

```text
用户进入比赛详情评分 tab
-> 给球员评分 8.5
-> 再次评分 9.0
-> 原评分被覆盖
-> 平均分和评分人数正确变化
-> 用户不评分也可以评论
```

### 7.7 数据页场景

```text
用户进入数据 tab
-> 默认定位最近一场比赛
-> 查看重要比赛
-> 查看关注球队赛程
-> 切换赛事
-> 查看积分榜、球员榜、球队榜
```

### 7.8 我的页面场景

```text
用户进入我的页面
-> 看台显示主队、关注球队、关注球员
-> 发布 tab 显示用户内容
-> 点赞 tab 显示点赞内容
-> 收藏 tab 显示收藏内容
-> 评论 tab 显示评论记录
```

## 8. P2 预留场景测试

当前不强制执行，但保留测试设计。

### 8.1 约球场景

```text
用户创建约球活动
-> 选择认证球场
-> 其他用户报名
-> 查看约球详情
-> 进入约球聊天室
-> 活动结束
```

### 8.2 私信场景

```text
用户进入消息列表
-> 选择某个用户会话
-> 发送文字或内容卡片
-> 对方收到消息
```

私信当前待确认，不进入第一版强制测试。

## 9. Docker 构建验收计划

```powershell
cd D:\south-stand-dev\south-stand-backend

mvn clean package -DskipTests
docker build -t south-stand-backend:0.1.0 .
docker images | findstr south-stand-backend
docker save -o south-stand-backend-0.1.0.tar south-stand-backend:0.1.0
```

通过标准：生成 `south-stand-backend-0.1.0.tar`。

## 10. Linux 部署验收计划

导入镜像：

```bash
docker load -i south-stand-backend-0.1.0.tar
```

启动：

```bash
cd /opt/south-stand
docker compose up -d
docker compose ps
```

查看日志：

```bash
docker logs -f south-stand-backend
```

通过标准：mysql、redis、backend 都是 Up。

## 11. 建议脚本

建议创建：

```text
scripts/windows/check-repo.ps1
scripts/windows/check-t01.ps1
scripts/windows/check-t02.ps1
scripts/windows/reset-dev-db.ps1
scripts/check-backend.ps1
scripts/check-smoke.ps1
scripts/check-docker-build.ps1
scripts/check-deploy-health.sh
```

`scripts/windows/check-repo.ps1` 覆盖：

```text
Git 仓库、分支、remote
docs/tmp/README/.gitignore
jar 直跑部署文档
T00 任务计划
.env 和 tmp Git 跟踪状态
T00 禁止创建的 pom.xml、src/**
```

`scripts/windows/check-t01.ps1` 覆盖：

```text
T01 项目骨架文件检查
mvn clean test
mvn clean package
jar 产物检查
java -jar 启动
/api/public/health smoke
```

`scripts/windows/check-t02.ps1` 覆盖：

```text
本地开发库重置
schema / seed 执行
mvn clean test
mvn clean package
jar 启动
/api/public/health
/api/public/health/db
/api/public/health/redis
```

`check-backend.ps1` 覆盖：

```text
mvn clean test
mvn clean package
```

`check-smoke.ps1` 覆盖：

```text
health
login
onboarding
feed
content detail
comments
football leagues
team detail
player detail
match detail
my profile
```

`check-docker-build.ps1` 覆盖：

```text
mvn package
docker build
docker images
docker save
```

## 12. 提交前检查清单

每次提交前检查：

```text
git status
mvn test
mvn package
核心接口 smoke
无 .env
无真实密码
无 docker/data
无 uploads 大文件
数据库文档与 schema.sql 一致
接口文档与代码一致
```

## 13. 第一版完成标准

| 项 | 标准 |
|---|---|
| 编译 | `mvn package` 成功 |
| 启动 | 本地和 Docker 均可启动 |
| 接口文档 | Knife4j 可访问 |
| 登录 | 注册、登录、当前用户可用 |
| 首次登录 | 主队、关注球队、关注球员可设置 |
| 首页 | 推荐、资讯、关注、球队 tab 基础可用 |
| 内容 | 首页、详情、发布可用 |
| 互动 | 评论、二级回复、点赞、收藏可用 |
| 足球数据 | 联赛、球队、球员、比赛、赛程可用 |
| 球队详情 | 总览、帖子、球员、赛程基础可用 |
| 球员详情 | 头部、帖子、数据基础可用 |
| 比赛详情 | 总览、事件、战报、统计基础可用 |
| 我的 | 看台、发布、点赞、收藏、评论可用 |
| 管理后台 | 管理员能维护内容和足球数据 |
| 部署 | Linux Docker Compose 运行成功 |
| 文档 | 12 个主线文档存在并与代码一致 |
## T05 Content And Interaction Validation

Run:

```powershell
cd D:\Football-APP
.\scripts\windows\check-t05.ps1
```

The T05 check covers:

```text
reset-dev-db.ps1
mvn clean test
mvn clean package
jar startup
smoke-auth.ps1
smoke-onboarding.ps1
smoke-content.ps1
```

`smoke-content.ps1` verifies seed content `20001`, seed comments, post creation, content detail, root comment, reply, content like/unlike, comment like, content favorite/unfavorite, and no-token write rejection with `40101`.

## T06 Football Data Validation

Run:

```powershell
cd D:\Football-APP
.\scripts\windows\check-t06.ps1
```

The T06 check covers:

```text
reset-dev-db.ps1
mvn clean test
mvn clean package
jar startup
smoke-auth.ps1
smoke-onboarding.ps1
smoke-content.ps1
smoke-football.ps1
```

`smoke-football.ps1` verifies league list, important matches, match filters by team and league, team detail, player detail, match detail eventList and report entry, following-team schedule with token, and following-team schedule without token returning `40101`.

## T07 Feed Rule Recommendation Validation

Run:

```powershell
cd D:\Football-APP
.\scripts\windows\check-t07.ps1
```

The T07 check covers:

```text
reset-dev-db.ps1
mvn clean test
mvn clean package
jar startup
smoke-auth.ps1
smoke-onboarding.ps1
smoke-content.ps1
smoke-football.ps1
smoke-feed.ps1
```

`smoke-feed.ps1` verifies default feed, recommend/news/match/mixed tabs, hot leagues, token-based recommend and following feeds, team and league filters, and `pageSize` capped at 100.

## T08 User Center And Admin Validation

Run:

```powershell
cd D:\Football-APP
.\scripts\windows\check-t08.ps1
```

The T08 check covers:

```text
reset-dev-db.ps1
mvn clean test
mvn clean package
jar startup
smoke-auth.ps1
smoke-onboarding.ps1
smoke-content.ps1
smoke-football.ps1
smoke-feed.ps1
smoke-user-admin.ps1
```

`smoke-user-admin.ps1` verifies my summary/profile/contents/favorites/comments, no-token `40101`, admin dashboard/users/contents, user disable and enable, content hide and restore, hidden content detail returning `40401`, ordinary USER admin access returning `40301`, and admin no-token access returning `40101`.

## T09 File Upload Security Validation

Run:

```powershell
cd D:\Football-APP
.\scripts\windows\check-t09.ps1
```

The T09 check covers:

```text
reset-dev-db.ps1
mvn test
mvn clean package
jar startup with APP_FILE_STORAGE_ROOT pointing to a temp directory
smoke-auth.ps1
smoke-football.ps1
smoke-feed.ps1
smoke-file-upload.ps1
```

`smoke-file-upload.ps1` registers and logs in a user, generates a local 1x1 PNG in a temp directory, uploads it, verifies `/api/public/files/{fileId}` returns 200 and `nosniff`, verifies no-token upload returns `40101`, and verifies `.txt` upload returns `40001`.

## T10 Storage Abstraction And Media Binding Validation

Run:

```powershell
cd D:\Football-APP
.\scripts\windows\check-t10.ps1
```

The T10 check covers:

```text
reset-dev-db.ps1
mvn test
mvn clean package
jar startup with APP_FILE_STORAGE_TYPE=LOCAL and temp APP_FILE_LOCAL_STORAGE_ROOT
smoke-auth.ps1
smoke-football.ps1
smoke-feed.ps1
smoke-file-upload.ps1
smoke-storage-media.ps1
```

`smoke-storage-media.ps1` verifies LOCAL storageType in upload responses, avatar binding by fileId, content post creation with mediaFileIds, content detail mediaList containing uploaded URLs, file soft delete, and deleted public file access returning 404.

## T11 User Social Validation

Run:

```powershell
cd D:\Football-APP
.\scripts\windows\check-t11.ps1
```

The T11 check covers:

```text
reset-dev-db.ps1
mvn test
mvn clean package
jar startup with APP_FILE_STORAGE_TYPE=LOCAL and temp APP_FILE_LOCAL_STORAGE_ROOT
smoke-auth.ps1
smoke-football.ps1
smoke-feed.ps1
smoke-file-upload.ps1
smoke-storage-media.ps1
smoke-user-social.ps1
```

`smoke-user-social.ps1` verifies public user profile, token-aware relationStatus, follow/unfollow, mutual relationship, followers/followings pages, public user contents, my stand, following feed after following a user, other-user favorites returning `40301`, and self follow returning `40001`.

## T12 Comment Hot Validation

Run:

```powershell
cd D:\Football-APP
.\scripts\windows\check-t12.ps1
```

The T12 check covers:

```text
reset-dev-db.ps1
mvn test
mvn clean package
jar startup with APP_FILE_STORAGE_TYPE=LOCAL and temp APP_FILE_LOCAL_STORAGE_ROOT
smoke-auth.ps1
smoke-football.ps1
smoke-feed.ps1
smoke-file-upload.ps1
smoke-storage-media.ps1
smoke-user-social.ps1
smoke-comment-hot.ps1
```

`smoke-comment-hot.ps1` verifies post creation, root comment creation, reply creation, comment like toggle, hot/latest comment lists, replies endpoint, hot comments endpoint, feed `hotComment`, comment soft delete, deleted comments disappearing from normal lists, and deleted comment like returning `40401`.
