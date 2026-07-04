# 南看台登录鉴权与基础安全规范

> 版本：v0.2-tifo-revised  
> 定位：定义南看台第一版登录、鉴权、密码加密、接口权限和基础安全规范。第一版目标是快速保证基础安全，不做复杂企业级权限系统。

## 1. 第一版安全目标

必须保证：

1. 密码不明文存储；
2. 登录后使用 JWT 鉴权；
3. 普通用户不能访问管理后台；
4. 被禁用用户不能登录；
5. 敏感配置不提交 Git；
6. 密码、Token、密钥不打印日志；
7. 基础接口有参数校验；
8. 登录失败有简单防刷；
9. 文件上传只允许安全类型；
10. 业务约束能被后端校验，例如有效球队、有效球员、不能关注自己。

## 2. 用户角色

| 角色 | 说明 |
|---|---|
| `USER` | 普通用户 |
| `ADMIN` | 管理员 |

第一版不做复杂 RBAC，不设计 `sys_role`、`sys_permission`、`sys_role_permission`。

## 3. 密码加密

使用：

```text
BCrypt
```

禁止：

```text
明文密码
MD5
SHA1
自定义弱加密
```

数据库字段：

```text
sys_user.password_hash
```

校验流程：

```text
用户输入密码
-> BCrypt matches(rawPassword, passwordHash)
-> 校验通过生成 JWT
```

## 4. JWT 鉴权

第一版只做 Access Token，不做 Refresh Token。

当前 T03 实现中，JWT Secret 必须通过环境变量读取：

```text
JWT_SECRET
JWT_ACCESS_TOKEN_EXPIRE_SECONDS
```

开发环境提供占位默认值，生产环境必须由 Linux 本地 `.env` 或 shell 环境覆盖。

Token 有效期建议：

| 端 | 有效期 |
|---|---:|
| App 端 | 7 天 |
| 管理后台 | 1 天 |

JWT payload 建议包含：

```json
{
  "userId": 10002,
  "username": "test_user",
  "roleType": "USER"
}
```

不要包含密码、手机号完整信息、身份证、敏感配置。

请求头：

```http
Authorization: Bearer <access_token>
```

## 5. 接口权限

### 5.1 公共接口

不需要登录：

```text
POST /api/auth/register
POST /api/auth/login
GET  /api/public/**
GET  /api/app/feed
GET  /api/app/contents/{contentId}
GET  /api/app/football/**
```

### 5.2 登录用户接口

需要登录：

```text
POST /api/app/onboarding/preferences
POST /api/app/comments
POST /api/app/likes/toggle
POST /api/app/favorites/toggle
POST /api/app/follows/toggle
POST /api/app/football/matches/*/players/*/rating
GET  /api/app/users/me/**
GET  /api/app/messages/**
POST /api/app/pickup-activities/**
```

### 5.3 管理员接口

需要 `ADMIN`：

```text
/api/admin/**
```

## 6. 当前用户上下文

建议封装：

```text
LoginUserContext
```

包含：

```text
userId
username
roleType
```

Service 层获取当前用户，不要在 Controller 到处解析 JWT。

## 7. 首次登录安全与业务校验

首次登录偏好设置需要校验：

```text
mainTeamId 必须是有效球队 ID
followPlayerIds 不允许重复
被禁用的球队/球员不能被关注
```

`sys_user.onboarding_completed` 与 `user_onboarding.completed` 要保持一致。

## 8. 关注业务约束

当前 T04 规则：

```text
关注球队不设置数量上限。
取消关注主队时，第一版不自动清空 mainTeamId，后续由产品再定。
```

## 9. 评分业务约束

球员评分需要校验：

```text
用户必须登录
比赛必须存在
球员必须属于该场比赛相关球队或名单
评分范围必须合法，例如 0~10 或 1~10
同一用户对同一比赛同一球员只能有一条有效评分
重复评分覆盖原评分
```

## 10. 登录失败防刷

第一版简单实现：

```text
Redis Key: login:fail:{username_or_ip}
规则：5 分钟内失败 5 次，锁定 10 分钟。
```

返回：

```json
{
  "code": 40103,
  "message": "登录失败次数过多，请稍后再试",
  "data": null
}
```

## 11. 敏感配置管理

不提交 Git：

```text
.env
application-prod.yml
*.local
docker/data/
uploads/
```

可以提交：

```text
.env.example
application-dev.yml
application-template.yml
docker-compose.example.yml
```

不得提交：

```text
数据库密码
Redis 密码
JWT Secret
服务器 IP 和密码
第三方 API Key
对象存储密钥
```

## 12. 日志安全

禁止打印：

```text
明文密码
password_hash
完整 JWT
JWT Secret
数据库密码
第三方 API Key
```

允许打印：

```text
userId
username
roleType
接口路径
错误码
traceId
```

Token 如果必须排查，只打印前后各 6 位。

## 13. 参数校验

Controller 请求 DTO 必须使用基础校验：

```text
@NotBlank
@NotNull
@Size
@Pattern
@Min
@Max
```

## 14. CORS

开发阶段允许：

```text
http://localhost:3000
http://localhost:5173
```

生产阶段不要直接允许 `*`。

## 15. 文件上传安全

限制：

| 项 | 规则 |
|---|---|
| 文件类型 | jpg、jpeg、png、webp、gif |
| 单文件大小 | 默认 10MB |
| 文件名 | 后端生成，不使用原始文件名作为存储名 |
| 存储目录 | uploads 按日期分目录 |
| 访问路径 | 只暴露相对 URL |

禁止上传：

```text
exe
sh
bat
jar
html
js
php
```

视频第一版不上传到本系统，只允许保存外部链接占位。

## 16. 第一版不做

```text
OAuth2
Refresh Token
多端设备管理
扫码登录
短信验证码
邮箱验证
复杂 RBAC
细粒度权限点
内容风控系统
设备指纹
行为风控
完整私信安全模型
```

## 17. 安全验收清单

| 检查项 | 标准 |
|---|---|
| 密码 | 数据库中看不到明文密码 |
| Token | 无 Token 不能访问需要登录的接口 |
| 管理后台 | USER 访问 `/api/admin/**` 返回无权限 |
| 禁用用户 | 禁用后不能登录 |
| 首次登录 | 非法球队/球员 ID 被拒绝 |
| 关注规则 | 第 6 支球队仍可关注成功，当前不设置数量上限 |
| 评分 | 重复评分覆盖而不是新增多条有效记录 |
| 日志 | 日志中无密码和完整 Token |
| Git | 仓库中无 `.env` 和真实密钥 |
| 文件上传 | 非图片文件被拒绝 |

T03 当前验收脚本：

```powershell
.\scripts\windows\check-t03.ps1
```

该脚本覆盖注册、登录、当前用户、无 Token、普通用户访问后台、管理员访问后台和登录失败防刷。
## T05 Permission Notes

T05 keeps read/write boundaries explicit:

```text
GET  /api/app/contents/{contentId} is public.
GET  /api/app/comments is public.
POST /api/app/contents/posts requires login.
POST /api/app/comments requires login.
POST /api/app/likes/toggle requires login.
POST /api/app/favorites/toggle requires login.
```

No-token write requests must return `40101`. Public reads may include an optional bearer token; when absent, user-specific interaction flags are false.

## T08 Permission Notes

T08 keeps user-center and admin permissions explicit:

```text
GET  /api/app/users/me/summary requires login.
PUT  /api/app/users/me/profile requires login.
GET  /api/app/users/me/contents requires login.
GET  /api/app/users/me/favorites requires login.
GET  /api/app/users/me/comments requires login.
GET  /api/admin/dashboard/summary requires ADMIN.
GET  /api/admin/users requires ADMIN.
PUT  /api/admin/users/{userId}/status requires ADMIN.
GET  /api/admin/contents requires ADMIN.
PUT  /api/admin/contents/{contentId}/status requires ADMIN.
```

No-token protected requests return `40101`; ordinary USER requests to admin endpoints return `40301`. User and admin response VOs do not expose password hashes, full JWTs, secrets, or unmasked phone numbers. Users disabled after token issuance are rejected by the T08 user-center service on subsequent `/api/app/users/me/**` requests; broader DB revalidation for every legacy endpoint remains a later hardening item.

## T09 File Upload Security Notes

Upload protection:

```text
POST /api/app/files/upload requires login and returns 40101 without token.
GET /api/public/files/{fileId} is public and returns binary data, not unified JSON.
Only AVATAR, CONTENT_IMAGE, COMMENT_IMAGE, and GENERAL_IMAGE biz types are accepted.
```

Validation strategy:

```text
Original file name is recorded only for display and metadata.
Disk file name is generated with UUID and never uses the original name.
Dangerous file names containing path separators, traversal, or shell-sensitive characters are rejected.
Extension, Content-Type, size, and magic number must all pass.
Allowed image types: jpg, jpeg, png, webp, gif.
Unsupported types such as svg, html, js, exe, sh, jar, and txt are rejected with 40001.
```

Response hardening:

```text
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: no-referrer
Cache-Control: no-store
```

CORS is configurable through `app.cors.*` and environment variables. With credentials enabled, the default allowed origins are explicit local frontend origins rather than `*`.

## T10 Storage And Media Security Notes

Storage configuration:

```text
APP_FILE_STORAGE_TYPE=LOCAL
APP_FILE_LOCAL_STORAGE_ROOT=uploads
```

Cloud storage placeholders exist for `ALIYUN_OSS`, `QINIU_KODO`, and `MINIO`, but no SDKs or real credentials are included. Placeholder secret settings must remain environment-only and default to empty values.

Media ownership rules:

```text
Avatar binding requires the file to belong to the current user and use AVATAR or GENERAL_IMAGE.
Content media binding requires the file to belong to the current user and use CONTENT_IMAGE or GENERAL_IMAGE.
File soft delete requires ownership and does not physically remove local files.
```

## T11 User Social Permission Notes

Public reads:

```text
GET /api/app/users/{userId}/profile is public.
GET /api/app/users/{userId}/contents is public.
GET /api/app/users/{userId}/followings is public.
GET /api/app/users/{userId}/followers is public.
```

Protected writes and private reads:

```text
POST   /api/app/users/{userId}/follow requires login.
DELETE /api/app/users/{userId}/follow requires login.
GET    /api/app/users/me/stand requires login.
GET    /api/app/users/{userId}/favorites requires login and only allows self.
GET    /api/app/users/{userId}/comments requires login and only allows self.
```

Visibility:

```text
DISABLED or deleted users return 40401 on public profile and list endpoints.
DISABLED or deleted current users cannot actively follow and receive 40101 from the user social service.
Self follow/unfollow returns 40001.
Other users' favorites/comments return 40301.
```

## T12 Comment Permission Notes

Public reads:

```text
GET /api/app/comments
GET /api/app/comments/{commentId}/replies
GET /api/app/comments/hot
```

Protected writes:

```text
POST   /api/app/comments requires login.
POST   /api/app/comments/{commentId}/likes/toggle requires login.
DELETE /api/app/comments/{commentId} requires login.
```

Deletion:

```text
The comment author can delete their own comment.
ADMIN can delete any comment.
Other users receive 40301.
Deleted comments return 40401 for like/reply flows and are not shown in normal lists.
```
