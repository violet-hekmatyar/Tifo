# 南看台 Codex 大任务开发计划

> 版本：v0.1  
> 定位：面向 Codex / Cursor / Copilot 等 AI Coding 工具的“大任务级”开发路线图。本文用于约束每一轮 AI Coding 的任务边界、上下文输入、输出格式和验收方式。  
> 注意：本文不替代 `02_REQUIREMENT_SCOPE.md`、`05_DATABASE_SCHEMA.md`、`06_API_SPEC.md`、`11_VALIDATION_AND_SMOKE_GUIDE.md`，也不写具体代码实现细节。

---

## 1. 文档定位

`12_CODEX_TASK_PLAN.md` 解决的问题是：

```text
已经有了 00-11 主线文档之后，
如何把这些文档转换成可控的 Codex 开发任务。
```

本文只拆“大任务”，不拆过细的函数、接口、类、Mapper、DTO、VO 等小任务。

原因：

1. 小任务需要根据代码实际生成情况继续调整；
2. 每轮 Codex Prompt 应该由 GPT 结合当前代码状态单独编写；
3. 大任务负责控制方向，小任务负责执行落地；
4. 避免一开始把任务切得过碎，导致上下文割裂、重复返工。

---

## 2. 总体开发原则

第一轮开发只追求：

```text
P0 可演示闭环
```

不追求：

```text
完整产品
复杂推荐算法
复杂实时通信
完整 IM
复杂约球聊天室
正式第三方体育数据 API
高并发压测
多服务微服务拆分
Kubernetes 部署
```

每一轮 Codex 开发必须遵守：

1. **先读文档，再写代码**。
2. **先做闭环，再补细节**。
3. **先能启动，再谈优化**。
4. **先保证契约稳定，再扩展接口**。
5. **每轮只做一个明确目标**。
6. **每轮必须给出测试方式**。
7. **每轮完成后必须说明修改文件、实现逻辑、风险点**。
8. **不得自行更换技术栈、数据库主结构、统一返回结构、鉴权方案**。
9. **不得把 P1 / P2 功能强行做成 P0**。
10. **不得把原型图中的不确定内容自行脑补成后端实现**。

---

## 3. Codex 稳定 Prompt 格式

后续每次让 Codex 开发，建议都采用以下稳定格式。

```text
当前目标：
说明本轮要完成的大任务或子任务，只写一个清晰目标。

项目背景：
南看台是一个卡片化足球内容流 + 赛事数据 + 社区互动 APP。
后端采用 Spring Boot 3.2.4 + JDK 17 + MySQL + Redis + MyBatis-Plus + Spring Security/JWT + Knife4j。
当前开发策略是先完成 P0 可演示闭环，不追求完整产品。

必须阅读：
- docs/00_DOCUMENT_MAP.md
- docs/01_PROJECT_OVERVIEW.md
- docs/02_REQUIREMENT_SCOPE.md
- docs/03_TECH_STACK.md
- docs/04_BACKEND_ARCHITECTURE.md
- docs/05_DATABASE_SCHEMA.md
- docs/06_API_SPEC.md
- docs/07_AUTH_SECURITY.md
- docs/08_ALGORITHM_INTEGRATION.md
- docs/09_DEPLOYMENT_GUIDE.md
- docs/10_AI_CODING_RULES.md
- docs/11_VALIDATION_AND_SMOKE_GUIDE.md
- docs/12_CODEX_TASK_PLAN.md

本轮重点参考：
根据本轮任务列出 3-6 个最相关文档。

允许修改：
明确允许修改的目录或文件。

禁止修改：
明确禁止修改的目录或文件。

实现范围：
说明本轮必须实现什么、可以占位什么、不要实现什么。

实现要求：
说明必须遵守的统一返回、分页、错误码、DTO/VO、权限、异常处理、日志、安全、命名规范等。

验收方式：
列出必须执行的命令、必须访问的接口、必须生成的报告或日志。

输出要求：
1. 列出修改文件
2. 说明实现逻辑
3. 给出测试命令
4. 给出接口调用示例
5. 说明未完成项
6. 说明风险点
7. 说明是否建议提交 Git
```

### 3.1 Prompt 使用注意

每次 Prompt 不要写成：

```text
帮我把后端写完
把所有接口都实现
你看着做
随便优化
根据原型图直接实现
```

应写成：

```text
本轮只完成 xxx 闭环。
其他内容只允许占位，不允许扩展。
```

---

## 4. 大任务路线总览

建议第一轮 Codex 开发按照以下大任务推进：

| 编号 | 大任务 | 核心目标 | 是否 P0 |
|---|---|---|---|
| T00 | 仓库基础整理与部署口径调整 | 仓库、文档、忽略规则、检查脚本先稳定下来 | 是 |
| T01 | 项目骨架与基础配置 | 项目能启动、能编译、基础配置模板齐全 | 是 |
| T02 | 通用后端基础设施 | 统一返回、异常、错误码、分页、健康检查、基础 SQL | 是 |
| T03 | 登录鉴权与用户闭环 | 注册、登录、JWT、当前用户、管理员权限 | 是 |
| T04 | 首次登录与关注闭环 | 主队、关注球队、关注球员、当前无关注上限 | 是 |
| T05 | 内容与互动闭环 | 内容、详情、评论、回复、点赞、收藏 | 是 |
| T06 | 足球数据闭环 | 联赛、球队、球员、比赛、事件、战报基础接口 | 是 |
| T07 | 首页卡片流与规则推荐 | CONTENT_CARD、MATCH_CARD、recommend/news/following/team | 是 |
| T08 | 我的页面与管理后台闭环 | 我的页面聚合、后台内容和基础数据维护 | 是 |
| T09 | 文件上传与基础安全加固 | 图片上传、安全校验、日志脱敏、登录防刷 | 是 |
| T10 | Docker 构建与本地运行 | Dockerfile、compose 示例、本地镜像构建验证 | 是 |
| T11 | 专项测试与验证报告 | 单测、接口 smoke、场景测试、测试报告 | 是 |
| T12 | Linux 部署支持与上线记录 | 部署脚本模板、上线检查、部署记录模板 | P0/P1 |

说明：

```text
T00 是所有开发任务前的仓库准备任务，应先于 T01 完成。
T01-T11 是第一轮 P0 可演示闭环建议完成范围。
T12 是否纳入第一轮，取决于 Linux 服务器准备情况。
```

---

### 4.1 T00：仓库基础整理与部署口径调整

目标：

```text
让仓库、文档、忽略规则和检查脚本先稳定下来，再进入 Spring Boot 项目骨架开发。
```

本轮只做开发前准备：

```text
.gitignore
README.md
文档地图
部署路径文档
AI Coding 规则
验证与 smoke 文档
Codex 任务计划
Windows 仓库检查脚本
Linux 脚本说明模板
```

本轮不做：

```text
不初始化 Spring Boot 项目
不创建 pom.xml
不创建 src/**
不写 Java 业务代码
不建数据库表
不实现接口
不连接 MySQL / Redis
不自动 git add / git commit / git push
```

T00 通过标准：

```text
scripts/windows/check-repo.ps1 执行成功
当前分支为 main
origin 指向 https://github.com/violet-hekmatyar/Tifo.git
.gitignore 已忽略 tmp/、.env、target/、logs/、uploads/
README.md 存在
docs/09_DEPLOYMENT_GUIDE.md 已补充当前阶段 jar 直跑路径
docs/12_CODEX_TASK_PLAN.md 已补充 T00
.env 未被 Git 跟踪
tmp 下文件未被 Git 跟踪
没有创建 src/**、pom.xml 或业务代码
```

---

## 5. T01：项目骨架与基础配置

### 5.1 目标

完成后端项目初始化，让项目具备最小可运行能力。

本轮不实现复杂业务，只实现：

```text
Spring Boot 项目骨架
Maven 依赖
基础包结构
配置文件模板
.gitignore
.env.example
application-template.yml
健康检查占位
Knife4j 接口文档入口
```

当前 T01 状态：

```text
初始化 Spring Boot 3.2.4 + JDK 17 Maven 项目
固定 jar 名称 south-stand-server.jar
提供 /api/public/health 最小健康检查
提供 scripts/windows/check-t01.ps1 验收脚本
当前不连接 MySQL / Redis
当前不创建 Dockerfile / docker-compose.yml
当前不创建 schema.sql / seed.sql
```

### 5.2 重点参考文档

```text
docs/03_TECH_STACK.md
docs/04_BACKEND_ARCHITECTURE.md
docs/07_AUTH_SECURITY.md
docs/09_DEPLOYMENT_GUIDE.md
docs/10_AI_CODING_RULES.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 5.3 建议允许修改

```text
pom.xml
README.md
.gitignore
.env.example
src/main/java/**
src/main/resources/**
http/**
scripts/**
```

### 5.4 必须产出

```text
基础 Spring Boot 项目
JDK 17
Spring Boot 3.2.4
MyBatis-Plus 依赖
MySQL Driver
Redis 依赖
Spring Security 依赖
JWT 依赖
Knife4j 依赖
Lombok 依赖
基础包结构
application-dev.yml
application-template.yml
.env.example
.gitignore
/api/public/health
```

### 5.5 配置留白原则

`.env.example`、`application-template.yml`、`application-prod.template.yml` 中可以保留占位值。

示例：

```text
change_me
your_mysql_password
your_jwt_secret
your_server_host
```

不得写入真实密码、真实服务器 IP、真实 Token、真实密钥。

### 5.6 验收方式

```powershell
mvn clean test
mvn clean package
mvn spring-boot:run
```

当前 T01 标准验收命令：

```powershell
.\scripts\windows\check-t01.ps1
```

接口：

```http
GET /api/public/health
```

通过标准：

```text
项目能启动
mvn package 成功
health 接口返回统一结构
Knife4j 页面可访问
```

---

## 6. T02：通用后端基础设施

### 6.1 目标

建立后续业务模块共用的后端基础设施。

包括：

```text
统一返回 Result
统一分页 PageResult
统一错误码 ErrorCode
业务异常 BusinessException
全局异常处理 GlobalExceptionHandler
请求参数校验基础配置
traceId/requestId 占位
数据库连接配置
Redis 连接配置
schema.sql
seed.sql
```

当前 T02 状态：

```text
恢复 MySQL DataSource 自动配置
接入 Redis 基础配置
完善 Result / PageResult / ErrorCode / BusinessException / GlobalExceptionHandler
加入 X-Trace-Id / X-Request-Id 基础 traceId 能力
新增 /api/public/health/db 和 /api/public/health/redis
新增 scripts/sql/schema.sql、seed.sql、reset-dev.sql
新增 scripts/windows/reset-dev-db.ps1 和 check-t02.ps1
当前仍不实现注册、登录、JWT Filter 或业务接口
```

### 6.2 重点参考文档

```text
docs/05_DATABASE_SCHEMA.md
docs/06_API_SPEC.md
docs/07_AUTH_SECURITY.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 6.3 建议允许修改

```text
src/main/java/com/southstand/common/**
src/main/java/com/southstand/infrastructure/**
src/main/resources/**
scripts/sql/**
http/**
```

### 6.4 必须产出

```text
Result<T>
PageResult<T>
ErrorCode
BusinessException
GlobalExceptionHandler
基础枚举
基础常量
数据库配置
Redis 配置
scripts/sql/schema.sql
scripts/sql/seed.sql
scripts/sql/reset-dev.sql 可选
/api/public/health/db
/api/public/health/redis
```

### 6.5 SQL 要求

`schema.sql` 需要优先覆盖 P0 表：

```text
sys_user
user_profile
user_onboarding
content
content_media
content_relation
comment
like_record
favorite_record
follow_record
football_league
football_team
football_player
team_player
match_info
match_event
match_report
admin_operation_log
```

`seed.sql` 至少包含：

```text
管理员账号
普通用户
联赛
球队
球员
比赛
内容
评论
关注关系
点赞收藏样例
```

### 6.6 验收方式

```powershell
mvn clean test
mvn clean package
```

当前 T02 标准验收命令：

```powershell
.\scripts\windows\check-t02.ps1
```

接口：

```http
GET /api/public/health
GET /api/public/health/db
GET /api/public/health/redis
```

通过标准：

```text
统一返回结构稳定
错误码稳定
数据库能连接
Redis 能连接
schema.sql 可执行
seed.sql 可执行
```

---

## 7. T03：登录鉴权与用户闭环

### 7.1 目标

实现用户注册、登录、JWT 鉴权、当前用户和管理员基础权限。

当前 T03 状态：

```text
已实现 POST /api/auth/register
已实现 POST /api/auth/login
已实现 GET /api/auth/me
已实现 JWT 生成、解析、过滤器和当前用户上下文
已实现 /api/admin/health 管理员权限占位
已实现 Redis 登录失败防刷
已新增 scripts/windows/smoke-auth.ps1 和 check-t03.ps1
当前仍不实现首次登录偏好、关注、内容、评论、足球数据或后台业务管理
```

### 7.2 重点参考文档

```text
docs/05_DATABASE_SCHEMA.md
docs/06_API_SPEC.md
docs/07_AUTH_SECURITY.md
docs/10_AI_CODING_RULES.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 7.3 建议允许修改

```text
src/main/java/com/southstand/auth/**
src/main/java/com/southstand/user/**
src/main/java/com/southstand/common/**
src/main/java/com/southstand/infrastructure/cache/**
src/main/resources/**
http/**
```

### 7.4 必须实现

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
JWT 生成
JWT 解析
Spring Security 基础过滤
BCrypt 密码加密
普通用户权限
管理员权限
禁用用户不能登录
```

### 7.5 不做内容

```text
Refresh Token
短信验证码
邮箱验证
OAuth2
扫码登录
复杂 RBAC
多端设备管理
```

### 7.6 验收方式

```powershell
mvn clean test
mvn clean package
```

接口：

```http
POST /api/auth/register
POST /api/auth/login
GET  /api/auth/me
GET  /api/admin/** 使用普通用户 Token 应返回无权限
```

通过标准：

```text
密码不明文入库
登录返回 JWT
JWT 可访问登录接口
普通用户不能访问管理后台
管理员可访问管理后台占位接口
```

---

## 8. T04：首次登录与关注闭环

### 8.1 目标

实现首次登录偏好选择，包括：

```text
我的主队
关注球队
关注球员
首次登录完成状态
关注球队数量当前不设上限
```

当前 T04 状态：

```text
已实现 GET /api/app/onboarding/options
已实现 POST /api/app/onboarding/preferences
已实现 POST /api/app/follows/toggle
已实现 GET /api/app/users/me/profile
已补充 seed 到 3 个赛事、6 支球队、10 名球员和 10 条 team_player 关系
已新增 scripts/windows/smoke-onboarding.ps1 和 check-t04.ps1
当前关注球队不设置数量上限
当前仍不实现首页 feed、内容详情、评论、点赞、收藏、足球数据详情或后台 CRUD
```

### 8.2 重点参考文档

```text
docs/02_REQUIREMENT_SCOPE.md
docs/05_DATABASE_SCHEMA.md
docs/06_API_SPEC.md
docs/07_AUTH_SECURITY.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 8.3 建议允许修改

```text
src/main/java/com/southstand/onboarding/**
src/main/java/com/southstand/follow/**
src/main/java/com/southstand/user/**
src/main/java/com/southstand/football/team/**
src/main/java/com/southstand/football/player/**
http/**
```

### 8.4 必须实现

```text
GET  /api/app/onboarding/options
POST /api/app/onboarding/preferences
POST /api/app/follows/toggle
GET  /api/app/users/me/profile
```

业务约束：

```text
mainTeamId 必须是有效球队
followTeamIds 去重处理，当前不设置数量上限
followPlayerIds 不重复
重复关注要幂等
被禁用球队/球员不能关注
sys_user.onboarding_completed 与 user_onboarding.completed 保持一致
```

### 8.5 不做内容

```text
复杂关注推荐算法
复杂粉丝关系推荐
私信关系
关注分组
```

### 8.6 验收方式

```text
注册用户
登录
获取 onboarding options
保存主队、关注球队、关注球员
查询当前用户资料
验证 onboardingCompleted=true
验证第 6 支球队仍可关注成功
```

---

## 9. T05：内容与互动闭环

### 9.1 目标

实现内容详情、用户发帖、后台文章/战报发布的基础能力，以及评论、二级回复、点赞、收藏。

### 9.2 重点参考文档

```text
docs/02_REQUIREMENT_SCOPE.md
docs/05_DATABASE_SCHEMA.md
docs/06_API_SPEC.md
docs/07_AUTH_SECURITY.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 9.3 建议允许修改

```text
src/main/java/com/southstand/content/**
src/main/java/com/southstand/interaction/**
src/main/java/com/southstand/admin/**
src/main/java/com/southstand/common/**
http/**
```

### 9.4 必须实现

```text
GET  /api/app/contents/{contentId}
POST /api/app/contents/posts
GET  /api/app/comments
POST /api/app/comments
POST /api/app/likes/toggle
POST /api/app/favorites/toggle
```

内容能力：

```text
NEWS
POST
ARTICLE
REPORT
DISCUSS 占位
POST_FORMAT
ARTICLE_FORMAT
媒体列表
内容关联球队/球员/比赛/热点事件
用户点赞状态
用户收藏状态
```

互动能力：

```text
根评论
二级回复
评论点赞
内容点赞
内容收藏
评论热度排序
```

### 9.5 不做内容

```text
复杂内容审核
复杂富文本编辑器
视频上传
GIF 版权资源
AI 自动识别标签
完整内容风控
```

### 9.6 验收方式

```text
查询内容详情
发布帖子
评论内容
回复评论
点赞内容
收藏内容
点赞评论
确认计数变化
确认重复点赞/收藏是 toggle 语义
```

---

## 10. T06：足球数据闭环

### 10.1 目标

实现足球垂直 APP 的基础数据能力：

```text
联赛
球队
球员
比赛
赛程
比赛事件
比赛战报
基础榜单占位
```

### 10.2 重点参考文档

```text
docs/02_REQUIREMENT_SCOPE.md
docs/05_DATABASE_SCHEMA.md
docs/06_API_SPEC.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 10.3 建议允许修改

```text
src/main/java/com/southstand/football/**
src/main/java/com/southstand/admin/**
src/main/java/com/southstand/content/**
http/**
scripts/sql/**
```

### 10.4 必须实现

```text
GET /api/app/football/leagues
GET /api/app/football/matches/important
GET /api/app/football/matches/following-teams
GET /api/app/football/matches
GET /api/app/football/teams/{teamId}
GET /api/app/football/teams/{teamId}/overview
GET /api/app/football/teams/{teamId}/contents
GET /api/app/football/teams/{teamId}/players
GET /api/app/football/teams/{teamId}/matches
GET /api/app/football/players/{playerId}
GET /api/app/football/players/{playerId}/overview
GET /api/app/football/players/{playerId}/contents
GET /api/app/football/players/{playerId}/matches
GET /api/app/football/matches/{matchId}
GET /api/app/football/matches/{matchId}/overview
GET /api/app/football/matches/{matchId}/stats
```

可以先占位：

```text
积分榜
球员榜
球队榜
阵容
评分
生涯数据
复杂统计
```

### 10.5 不做内容

```text
第三方体育数据 API 正式接入
实时比分 WebSocket
自动爬虫
自动战报生成
GIF 集锦抓取
复杂淘汰赛树交互
```

### 10.6 验收方式

```text
能查询联赛
能查询球队
能查询球员
能查询比赛
能查询重要比赛
能查询关注球队赛程
能查询球队详情
能查询球员详情
能查询比赛详情
战报能与比赛绑定
```

---

## 11. T07：首页卡片流与规则推荐

### 11.1 目标

实现首页 feed 的基础卡片流能力。

第一版至少支持：

```text
CONTENT_CARD
MATCH_CARD
```

并支持：

```text
recommend
news
following
team
```

四类 tab。

### 11.2 重点参考文档

```text
docs/02_REQUIREMENT_SCOPE.md
docs/04_BACKEND_ARCHITECTURE.md
docs/05_DATABASE_SCHEMA.md
docs/06_API_SPEC.md
docs/08_ALGORITHM_INTEGRATION.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 11.3 建议允许修改

```text
src/main/java/com/southstand/card/**
src/main/java/com/southstand/recommend/**
src/main/java/com/southstand/content/**
src/main/java/com/southstand/football/**
src/main/java/com/southstand/follow/**
http/**
```

### 11.4 必须实现

```text
GET /api/app/feed
GET /api/app/feed/hot-leagues
```

推荐规则先使用规则排序：

```text
主队权重
关注球队权重
关注球员权重
关注用户权重
内容热度
发布时间衰减
重要比赛权重
卡片类型权重
```

返回结构必须遵守：

```text
cardType
targetType
targetId
sortScore
payload
```

### 11.5 tab 规则

```text
recommend：内容卡片 + 比赛卡片混排
news：只返回资讯/文章/帖子类内容卡片
following：关注用户内容，不足时兜底高热内容
team：指定 teamId 时返回该球队相关内容
```

### 11.6 不做内容

```text
复杂推荐模型
向量召回
算法服务正式接入
个性化深度学习排序
复杂去重策略
广告卡片
```

### 11.7 验收方式

```http
GET /api/app/feed?tab=recommend&pageNum=1&pageSize=10
GET /api/app/feed?tab=news&pageNum=1&pageSize=10
GET /api/app/feed?tab=following&pageNum=1&pageSize=10
GET /api/app/feed?tab=team&teamId=30001&pageNum=1&pageSize=10
```

通过标准：

```text
recommend 能返回多类型卡片
news 不返回 MATCH_CARD
following 没有关注内容时不空白
team 能返回指定球队相关内容
payload 不直接暴露 Entity
```

---

## 12. T08：我的页面与管理后台闭环

### 12.1 目标

完成普通用户“我的页面”基础聚合，以及管理员后台基础维护能力。

### 12.2 重点参考文档

```text
docs/01_PROJECT_OVERVIEW.md
docs/02_REQUIREMENT_SCOPE.md
docs/05_DATABASE_SCHEMA.md
docs/06_API_SPEC.md
docs/07_AUTH_SECURITY.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 12.3 建议允许修改

```text
src/main/java/com/southstand/user/**
src/main/java/com/southstand/admin/**
src/main/java/com/southstand/content/**
src/main/java/com/southstand/football/**
src/main/java/com/southstand/interaction/**
http/**
```

### 12.4 必须实现

App 端：

```text
GET /api/app/users/me/profile
GET /api/app/users/me/stand
GET /api/app/users/me/contents
GET /api/app/users/me/likes
GET /api/app/users/me/favorites
GET /api/app/users/me/comments
```

管理端：

```text
GET  /api/admin/contents
POST /api/admin/contents
GET  /api/admin/football/teams
POST /api/admin/football/teams
GET  /api/admin/football/players
POST /api/admin/football/players
GET  /api/admin/football/matches
POST /api/admin/football/matches
```

### 12.5 不做内容

```text
复杂运营后台
复杂审核流
复杂权限点
运营数据大屏
复杂用户风控
```

### 12.6 验收方式

```text
普通用户能查看我的资料
普通用户能查看自己的发布、点赞、收藏、评论
管理员能发布内容
管理员能维护球队、球员、比赛
普通用户访问 /api/admin/** 返回无权限
```

---

## 13. T09：文件上传与基础安全加固

### 13.1 目标

补齐第一版必要的文件上传能力和安全底线。

### 13.2 重点参考文档

```text
docs/03_TECH_STACK.md
docs/05_DATABASE_SCHEMA.md
docs/06_API_SPEC.md
docs/07_AUTH_SECURITY.md
docs/09_DEPLOYMENT_GUIDE.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 13.3 建议允许修改

```text
src/main/java/com/southstand/infrastructure/file/**
src/main/java/com/southstand/common/**
src/main/java/com/southstand/auth/**
src/main/resources/**
http/**
```

### 13.4 必须实现

```text
POST /api/file/upload/image
文件后缀校验
文件大小限制
后端生成文件名
按日期分目录
返回相对 URL
日志脱敏
基础 CORS 配置
登录失败 Redis 防刷
请求参数校验
```

### 13.5 不做内容

```text
视频上传
对象存储正式接入
MinIO 正式接入
图片审核
图片压缩
CDN
```

### 13.6 验收方式

```text
允许上传 jpg/png/webp/gif
拒绝 exe/sh/bat/jar/html/js/php
超出大小返回参数错误
上传后返回相对 URL
日志不打印完整 Token、密码、密钥
登录失败过多返回 40103
```

---

## 14. T10：Docker 构建与本地运行

### 14.1 目标

完成本地 Docker 镜像构建和 docker compose 本地运行模板。

### 14.2 重点参考文档

```text
docs/03_TECH_STACK.md
docs/09_DEPLOYMENT_GUIDE.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 14.3 建议允许修改

```text
Dockerfile
docker-compose.example.yml
.env.example
scripts/build.ps1
scripts/save-image.ps1
scripts/load-image.sh
README.md
```

### 14.4 必须实现

```text
Dockerfile
docker-compose.example.yml
Windows 构建脚本
Windows 镜像导出脚本
Linux 镜像导入脚本模板
运行目录说明
uploads volume
logs volume
```

### 14.5 重要边界

后端业务 jar 打进后端镜像。

MySQL、Redis 不打进后端镜像，应通过单独容器运行。

### 14.6 验收方式

```powershell
mvn clean package -DskipTests
docker build -t south-stand-backend:0.1.0 .
docker images | findstr south-stand-backend
docker save -o south-stand-backend-0.1.0.tar south-stand-backend:0.1.0
```

本地 compose 可选验证：

```powershell
docker compose -f docker-compose.example.yml up -d
docker compose ps
```

---

## 15. T11：专项测试与验证报告

### 15.1 目标

专门让 Codex 对项目进行测试验证，而不是在业务开发中顺手测试。

测试任务应作为独立大任务执行。

### 15.2 重点参考文档

```text
docs/06_API_SPEC.md
docs/07_AUTH_SECURITY.md
docs/09_DEPLOYMENT_GUIDE.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 15.3 建议允许修改

```text
src/test/**
http/**
scripts/test/**
logs/**
README.md
```

### 15.4 必须产出

```text
单元测试
接口 smoke 测试文件
测试脚本
测试报告
```

测试报告建议路径：

```text
logs/TEST_REPORT.md
```

如果需要多轮报告：

```text
logs/test/TEST_REPORT_YYYYMMDD.md
```

### 15.5 测试范围

必须覆盖：

```text
健康检查
注册登录
JWT 鉴权
管理员权限
首次登录偏好
关注球队数量无上限
首页 feed
内容详情
评论回复
点赞收藏
足球数据查询
球队详情
球员详情
比赛详情
我的页面
后台基础接口
Docker 构建
```

### 15.6 测试报告格式

```md
# TEST_REPORT

## 1. 测试时间

## 2. 测试环境

## 3. 测试命令

## 4. 测试结果总览

| 模块 | 结果 | 说明 |
|---|---|---|

## 5. 通过项

## 6. 失败项

## 7. 阻塞问题

## 8. 非阻塞问题

## 9. 建议修复顺序

## 10. 是否建议提交 Git
```

### 15.7 通过标准

```text
mvn clean test 通过
mvn clean package 通过
核心 smoke 接口通过
无明显鉴权绕过
无明文密码
无敏感配置提交
Docker 镜像可构建
```

---

## 16. T12：Linux 部署支持与上线记录

### 16.1 目标

为 Linux 上线提供脚本模板、检查清单和部署记录，但不要求 Codex 直接操作服务器。

### 16.2 重点参考文档

```text
docs/09_DEPLOYMENT_GUIDE.md
docs/11_VALIDATION_AND_SMOKE_GUIDE.md
```

### 16.3 建议允许修改

```text
scripts/deploy/**
logs/deploy/**
README.md
```

### 16.4 必须产出

```text
Linux 目录结构模板
docker compose 生产模板
镜像 load 脚本
服务启动检查脚本
日志查看命令
部署记录模板
回滚记录模板
```

部署记录建议路径：

```text
logs/deploy/DEPLOYMENT_RECORD.md
```

### 16.5 不做内容

```text
不在服务器上 git clone
不在服务器上 mvn package
不在服务器上 docker build 业务镜像
不把真实服务器密码写入仓库
不把真实 .env 提交 Git
```

### 16.6 验收方式

```text
脚本语法基本正确
部署文档能指导人工上线
上线后可按记录模板回填
```

---

## 17. 项目日志建议

项目中建议保留 `logs/` 目录，用于记录开发、测试、部署和决策。

建议结构：

```text
logs
├── DECISION_LOG.md
├── TEST_REPORT.md
├── codex
│   └── CODEX_CHANGE_REPORT_YYYYMMDD.md
└── deploy
    └── DEPLOYMENT_RECORD.md
```

### 17.1 DECISION_LOG.md

用于记录关键产品、技术和开发决策。

示例：

```md
# DECISION_LOG

## D001：第一版是否做完整私信

结论：
第一版不做完整 IM，只做系统通知、评论点赞提醒、关注提醒。

原因：
完整私信会引入会话、风控、举报审核、实时通信等复杂能力，第一版成本过高。

影响：
- 02_REQUIREMENT_SCOPE.md
- 05_DATABASE_SCHEMA.md
- 06_API_SPEC.md
```

### 17.2 CODEX_CHANGE_REPORT

每轮 Codex 修改后，建议让 Codex 输出变更报告。

建议格式：

```md
# CODEX_CHANGE_REPORT

## 1. 本轮目标

## 2. 修改文件

## 3. 实现内容

## 4. 测试命令

## 5. 测试结果

## 6. 未完成项

## 7. 风险点

## 8. 下一步建议

## 9. 是否建议提交 Git
```

---

## 18. Git 提交建议

每个大任务至少一次提交。

建议提交格式：

```text
chore: init backend skeleton
chore: add common result and sql scripts
feat: add auth and jwt flow
feat: add onboarding and follow flow
feat: add content interaction flow
feat: add football data flow
feat: add home feed card flow
feat: add user center and admin basics
chore: add docker packaging files
test: add smoke tests and test report
docs: add deployment records
```

每次提交前建议执行：

```powershell
git status
mvn clean test
mvn clean package
```

---

## 19. 第一轮最终验收清单

第一轮完成后，至少应该满足：

```text
服务能启动
Knife4j 能访问
schema.sql / seed.sql 能初始化数据库
用户能注册登录
JWT 鉴权可用
用户能设置主队、关注球队、关注球员
首页 feed 能返回 CONTENT_CARD / MATCH_CARD
内容详情能打开
评论、回复、点赞、收藏能跑通
球队、球员、比赛详情基础接口可用
我的页面基础接口可用
管理员能发布内容、维护球队/球员/比赛
mvn test 通过
mvn package 通过
Docker 镜像能构建
核心 smoke 测试有报告
```

---

## 20. 后续扩展边界

第一轮完成后，后续再考虑：

```text
球员评分完善
裁判评分
复杂榜单
复杂赛程筛选
淘汰赛树
热门讨论卡片
热门评论卡片
数据辩论场
推荐算法服务
第三方体育数据 API
MinIO / OSS
Elasticsearch
Kafka
ClickHouse
WebSocket 实时比分
完整私信 IM
约球聊天室
广告系统
会员支付
```

这些功能不得在第一轮 Codex 开发中自行提前实现。

---

## 21. 给 GPT 生成 Codex Prompt 时的建议

每次让 GPT 写 Codex Prompt 时，建议提供：

```text
1. 当前已经完成到哪个大任务
2. 当前代码目录结构
3. 当前报错或缺口
4. 本轮想完成哪个接口或闭环
5. 是否允许修改 SQL
6. 是否允许修改 API 文档
7. 本轮必须跑哪些测试
```

GPT 输出 Prompt 时，应继续使用第 3 节的稳定格式。

最终原则：

```text
人控制方向。
GPT 负责拆任务和写 Prompt。
Codex 负责执行代码修改。
人负责验收、运行、提交。
```
## T05 Current Completion Notes

T05 content and interaction closure is implemented with the following scope:

```text
Content detail
User post creation
Comment list with hot/time sorting
Root comment and second-level reply creation
Content/comment like toggle
Content favorite toggle
Seed content/media/relation/comment/like/favorite data
Windows smoke-content.ps1 and check-t05.ps1
```

Still out of scope for T05:

```text
Home feed
Complex recommendation
Admin content CRUD
File upload
Content audit workflow
Football detail pages
```

## T06 Current Completion Notes

T06 football data closure is implemented with the following scope:

```text
League list
Important match list
Following-team match schedule
Match list filters
Team basic detail
Player basic detail
Match basic detail
Match event list
Match report entry
Seed football data for leagues, teams, players, matches, events, and reports
Windows smoke-football.ps1 and check-t06.ps1
```

Still out of scope for T06:

```text
Home feed and recommendation
Full team detail tabs
Player career or complex statistics
Match lineups, ratings, standings, ranks, and complex stats
Admin CRUD
Realtime score push
Third-party sports API
```

## T07 Current Completion Notes

T07 feed rule recommendation closure is implemented with the following scope:

```text
Unified feed endpoint
Hot leagues endpoint
CONTENT and MATCH card VO protocol
recommend/following/news/match/mixed tabs
Rule scores for hot content, time boost, main team, followed teams, followed players, live matches, scheduled matches, finished reports, and important matches
Optional JWT personalization
Seed relation coverage for Barcelona team feed
Windows smoke-feed.ps1 and check-t07.ps1
```

Still out of scope for T07:

```text
Complex recommendation algorithm
Vector retrieval
Search API
Recommendation service integration
Admin CRUD
Realtime score push
Third-party sports API
```

## T08 Current Completion Notes

T08 user center and admin basic closure is implemented with the following scope:

```text
User-center summary endpoint
User profile update endpoint
My contents, my favorites, and my comments endpoints
Admin dashboard summary
Admin user list and ACTIVE/DISABLED status update
Admin content list and PUBLISHED/HIDDEN status update
Best-effort admin operation logs
Seed data for admin, active users, disabled user, profiles, user-authored content, favorites, comments, follows, and operation logs
Windows smoke-user-admin.ps1 and check-t08.ps1
```

Still out of scope for T08:

```text
Complex RBAC
Menu/button permissions
Department org structure
Content audit workflow
Rich text editor
File upload
Excel export
Notifications
Third-party login
Refresh token
```

## T09 Current Completion Notes

T09 file upload and basic security hardening is implemented with the following scope:

```text
Local disk image upload
file_resource metadata table
FileProperties and CORS configuration externalization
POST /api/app/files/upload
GET /api/public/files/{fileId}
Extension, MIME, size, file name, magic number, and path traversal validation
UUID storage file names under date directories
Basic security response headers
Windows smoke-file-upload.ps1 and check-t09.ps1
FileValidationServiceTests and FileStorageServiceTests
```

Still out of scope for T09:

```text
Video upload
Chunk upload
OSS / MinIO
Image compression
Image crop
Watermarking
Private file authorization
```

## T11 Current Completion Notes

T11 user social is implemented with the following scope:

```text
Public user profile by userId
User follow and unfollow through follow_record follow_type=USER
Follower and following lists
relationStatus values: SELF, NONE, FOLLOWING, FOLLOWED_BY, MUTUAL
Public user content list
Self-only favorites/comments under /api/app/users/{userId}
My stand endpoint
Following feed enhancement for followed users' content
Windows smoke-user-social.ps1 and check-t11.ps1
```

Still out of scope for T11:

```text
Private messages
Chat rooms
WebSocket
Notification fanout
Complex privacy settings
Real recommendation model training
```

## T12 Current Completion Notes

T12 comment hot is implemented with the following scope:

```text
Comment list sort=hot/latest
Hot score calculation: (likeCount + replyCount * 2) * timeFactor
Comment like/unlike endpoint reusing like_record target_type=COMMENT
Reply structure with rootId, parentId, replyToUserId, replyToNickname
Hot comments endpoint
Feed hotComment on content cards
Comment soft delete by author or ADMIN
User comment VO fields for root/reply/status
Windows smoke-comment-hot.ps1 and check-t12.ps1
```

Still out of scope for T12:

```text
WebSocket realtime comments
Private messages
Notification fanout
Complex moderation
AI review
Search or Redis hot ranking
```

## T10 Current Completion Notes

T10 storage abstraction and media business integration is implemented with the following scope:

```text
StorageService abstraction
LocalStorageService as default LOCAL storage
StorageServiceResolver for configured upload storage and historical file reads
ALIYUN_OSS / QINIU_KODO / MINIO placeholder services without SDK dependencies
file_resource storage metadata fields
Upload response storageType and objectKey
Avatar binding by fileId
Content image binding by mediaFileIds
File soft delete endpoint
Windows smoke-storage-media.ps1 and check-t10.ps1
StorageServiceResolverTests and LocalStorageServiceTests
```

Still out of scope for T10:

```text
Real Aliyun OSS SDK integration
Real Qiniu Kodo SDK integration
Real MinIO SDK integration
Physical file cleanup scheduler
Private file authorization
```
