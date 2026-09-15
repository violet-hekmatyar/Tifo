# Backend V1 前端快速启动

> 适用版本：Backend V1 Freeze（截至 2026-08-30）
> 完整接口与页面映射见 [FRONTEND_BACKEND_HANDOFF_V1.md](./FRONTEND_BACKEND_HANDOFF_V1.md)。

## 1. 环境版本

| 组件 | 项目要求/已验证版本 |
| --- | --- |
| JDK | 17（本机 Temurin 17.0.19） |
| Spring Boot | 3.2.4 |
| Maven | 3.9.16 |
| MySQL | 8.x（本机容器 `mysql:8.0`） |
| Redis | 7.x（本机容器 `redis:7`） |
| Python | 3.13.13 |

Python 依赖位于 `recommend-service/requirements.txt`，主要包括 FastAPI、Uvicorn、Pydantic 2、PyMySQL、pytest 和 httpx。

## 2. 检查 MySQL 与 Redis

项目开发环境默认连接：

- MySQL：`127.0.0.1:3306`，数据库名 `south_stand`
- Redis：`127.0.0.1:6379`，DB `0`

不要把数据库密码、JWT Secret 写入 Flutter 或提交到仓库。按本地环境设置 `MYSQL_USERNAME`、`MYSQL_PASSWORD`、`JWT_SECRET` 等变量。

如果使用项目当前 Docker 容器，可先检查：

```powershell
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}"
Test-NetConnection 127.0.0.1 -Port 3306
Test-NetConnection 127.0.0.1 -Port 6379
```

## 3. 启动 Python 推荐服务

在项目根目录执行：

```powershell
.\scripts\windows\start-recommend-service.ps1
```

服务地址为 `http://127.0.0.1:8100`。脚本以隐藏窗口启动 Uvicorn，并保存 PID 供停止脚本使用。

## 4. 检查 Python Health

```powershell
Invoke-RestMethod http://127.0.0.1:8100/health | ConvertTo-Json
```

关注字段：

- `modelReady=true`：CF 模型已准备好。
- `modelReady=false`：服务在线但模型未准备好，Java 会使用规则推荐降级。
- 8100 完全不可达：Java 同样自动降级，Flutter 首页不应因此不可用。

Flutter 不直接请求 8100。

## 5. 启动 Spring Boot

新开一个 PowerShell，仍在项目根目录执行：

```powershell
.\scripts\windows\run-dev.ps1
```

等价于使用 `dev` Profile 启动 `mvn spring-boot:run`。默认地址：

- API：`http://127.0.0.1:8080`
- Swagger：`http://127.0.0.1:8080/doc.html`

验证：

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/public/health | ConvertTo-Json
Invoke-RestMethod http://127.0.0.1:8080/api/public/health/db | ConvertTo-Json
Invoke-RestMethod http://127.0.0.1:8080/api/public/health/redis | ConvertTo-Json
```

Android 模拟器访问宿主机时使用 `http://10.0.2.2:8080`，或执行 `adb reverse tcp:8080 tcp:8080` 后使用 `http://127.0.0.1:8080`。真机应使用开发机局域网 IP。

## 6. JWT Header

注册或登录成功后保存 `data.accessToken`。需要登录的请求携带：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

默认有效期为 604800 秒（7 天）。V1 没有 refresh-token 和 logout API。遇到 `40101` 或 `40102` 时清理本地 Token，并返回登录页。

统一响应实际结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "..."
}
```

成功以 `code == 0` 判断，不要只判断 HTTP 200。分页 `data` 使用 `records`、`total`、`pageNum`、`pageSize`、`pages`。

## 7. 常用 API

| 场景 | 方法与路径 | 鉴权 |
| --- | --- | --- |
| 注册 | `POST /api/auth/register` | 否 |
| 登录 | `POST /api/auth/login` | 否 |
| 当前用户 | `GET /api/auth/me` | 是 |
| Onboarding 选项 | `GET /api/app/onboarding/options` | 是 |
| 提交偏好 | `POST /api/app/onboarding/preferences` | 是 |
| Feed | `GET /api/app/feed?tab=recommend&pageNum=1&pageSize=10` | 否 |
| 搜索 | `GET /api/app/search/entities?keyword=arsenal&pageNum=1&pageSize=20` | 否 |
| 内容详情 | `GET /api/app/contents/{id}` | 否 |
| 评论列表 | `GET /api/app/comments?contentId={id}&sort=hot&page=1&pageSize=20` | 否 |
| 联赛列表 | `GET /api/app/football/leagues` | 否 |

Feed 当前冻结卡片类型为：`CONTENT`、`MATCH`、`HOT_COMMENT`、`DISCUSSION`、`RANKING`、`PLAYER_RATING`。必须按 `cardType` 分发解析，并为未知类型提供降级占位，不能把全部 payload 强转成同一个 Model。

## 8. 停止服务

停止 Python：

```powershell
.\scripts\windows\stop-recommend-service.ps1
```

停止 Spring Boot：在运行 `run-dev.ps1` 的终端按 `Ctrl+C`。

检查端口是否残留：

```powershell
Get-NetTCPConnection -LocalPort 8080,8100 -State Listen -ErrorAction SilentlyContinue |
  Select-Object LocalPort,OwningProcess
```

如需结束确认属于本项目的残留进程，先查看命令行，再按 PID 停止：

```powershell
Get-CimInstance Win32_Process -Filter "ProcessId=<PID>" | Select-Object ProcessId,CommandLine
Stop-Process -Id <PID>
```

## 9. 最先阅读的完整说明

启动成功后阅读 [FRONTEND_BACKEND_HANDOFF_V1.md](./FRONTEND_BACKEND_HANDOFF_V1.md)，其中包含鉴权、错误码、页面到 API 映射、Feed 六类卡片、上传、通知、推荐归因与埋点、Demo 数据限制及延期能力。
