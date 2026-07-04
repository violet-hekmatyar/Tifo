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
.\scripts\windows\reset-dev-db.ps1
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
