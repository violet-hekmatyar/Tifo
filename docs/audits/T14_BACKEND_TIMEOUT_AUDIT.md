# T14 前端请求后端超时专项审计

审计日期：2026-08-01  
后端目录：`D:\Football-APP`

## 1. 前端日志说明

现有日志只包含 Dart VM Service 地址和 Android 模拟器的 `userfaultfd: MOVE ioctl seems unsupported: Connection timed out`。日志中没有 HTTP URL、Dio 异常类型、HTTP 状态码或请求耗时，因此不能单凭该日志认定后端接口超时。

## 2. userfaultfd 与 HTTP 的关系

该行来自 Android/模拟器运行时，不是后端 HTTP 客户端日志。它不能证明 DNS、TCP、HTTP 或服务端处理发生超时。本次结论以端口监听、curl 分阶段耗时、SQL 次数和服务端计时日志为准。

## 3. 前端 Base URL

Android Studio 标准模拟器推荐使用：

```text
http://10.0.2.2:8080
```

模拟器内的 `localhost` 和 `127.0.0.1` 指向模拟器自身。API 路径继续使用 `/api/...`。

## 4. 后端监听地址

启动当前 jar 后，`Get-NetTCPConnection` 实测为：

```text
LocalAddress LocalPort OwningProcess
::           8080      44948
```

项目没有设置 `server.address`，Spring Boot 在开发机上绑定通配地址。`127.0.0.1`、`localhost`、`172.29.80.1` 和宿主机局域网地址 `10.191.72.117` 均返回 HTTP 200，所以没有把 dev 配置改为重复的 `0.0.0.0`。生产配置未修改。

## 5. PID 与 jar

修复前基准进程为 PID `41424`，命令为 `java -jar target/south-stand-server.jar`，启动时间 `2026-08-01 12:21:46`。端到端复验进程为 PID `44948`，jar 为 `D:\Football-APP\target\south-stand-server.jar`。测试结束后进程已停止，8080/8090 均无监听残留。

## 6. 宿主机接口耗时

修复后端到端脚本结果如下，均为 HTTP 200。`max/p95` 在 5 次样本中等于最大值。

| 接口 | 冷请求 | 热请求平均 | 热请求 max/p95 | 响应大小 |
|---|---:|---:|---:|---:|
| health | 0.0038s | 0.0030s | 0.0036s | 222 B |
| login | 0.8170s | 0.0931s | 0.1781s | 619 B |
| feed | 0.2031s | 0.0632s | 0.0694s | 28,381 B |
| matches | 0.2243s | 0.1800s | 0.2067s | 9,494 B |
| team search | 0.0117s | 0.0124s | 0.0238s | 5,380 B |
| player search | 0.0890s | 0.0613s | 0.0635s | 5,697 B |
| content detail | 0.1960s | 0.2393s | 0.4362s | 1,471 B |
| comments | 0.1902s | 0.1813s | 0.2191s | 15,601 B |
| user profile | 0.0391s | 0.0151s | 0.0171s | 481 B |

登录首次请求包含类加载和 BCrypt 预热，连续请求稳定低于 500ms。所有接口的 DNS、连接、首字节和总耗时由 `benchmark-backend-api.ps1` 记录。

## 7. 模拟器连接方式

采用方案 A：模拟器使用 `10.0.2.2` 访问宿主机，后端保持通配监听。当前环境没有可执行的 `adb`，所以未建立或依赖 `adb reverse`。不要同时混用两套地址策略。

## 8. Windows 防火墙

当前以太网网络配置文件是 `Public`，未发现名称匹配 Java、8080、Spring 或 Tifo 的显式启用规则。宿主机局域网 IPv4 地址 `10.191.72.117:8080` 实测 HTTP 200，未证明防火墙正在阻断，因此没有关闭防火墙，也没有新增放行规则。

## 9. Android cleartext 风险

仓库中没有 `.dart`、`pubspec.yaml` 或 `AndroidManifest.xml`，无法直接确认 Flutter Android 的 `usesCleartextTraffic` 或 `networkSecurityConfig`。前端使用开发 HTTP 地址时，需要确认 Android 9+ 的开发构建允许访问 `http://10.0.2.2:8080`。这属于仍需前端核验的连接失败原因，不能归因于后端耗时。

## 10. SQL 次数

通过 dev MyBatis 日志中 `Preparing:` 行数统计：

| 接口 | 修复前单次 SQL | 修复后单次 SQL |
|---|---:|---:|
| Feed recommend 第 1 页 20 条 | 1406 | 约 13，常数级 |
| 比赛列表 20 条 | 121 | 121，当前耗时达标 |
| 搜索混合列表 20 条 | 155 | 按具体类型测试均达标 |
| 内容详情 | 9 | 9 |
| 评论列表 20 条 | 110 | 110，当前耗时达标 |
| 用户主页 | 7 | 7 |

Feed 已消除随 100 条候选内容和 80 场候选比赛线性增长的查询。比赛、评论等列表仍有结构优化空间，见遗留问题。

## 11. 慢 SQL 与 EXPLAIN

Feed 候选 Content 查询：`type=ALL`，`rows=131`，`Extra=Using where; Using filesort`。Match 查询：`type=ALL`，`rows=86`，`Extra=Using where; Using filesort`。当前扫描行数很小，批量化后 Feed 热请求约 63ms，未新增索引。

`content_relation` 批量查询使用 `idx_content_relation_content`，`type=range`，`rows=9`，`Extra=Using index condition; Using where`。评论批量查询当前选择 `idx_parent_id`，估算 `rows=365`。后续数据显著增长时，应再次用生产形态数据和 `EXPLAIN ANALYZE` 决定复合索引，而不是按字段清单盲目建索引。

## 12. N+1 结论

Feed 确认存在严重 N+1：每张内容卡逐条查询作者、关系名称、热评和互动状态；每张比赛卡逐条查询联赛、球队、事件、报告和球员。现已按候选 ID 批量查询并用 Map 回填，分页和评分规则未改变。`FeedPerformanceStructureTests` 用 40 条内容验证关键 Mapper 调用次数不随卡片数量增加。

## 13. 修改文件

本专项新增或修改：

```text
src/main/java/com/southstand/card/service/FeedService.java
src/main/java/com/southstand/common/config/RequestTimingFilter.java
src/main/resources/application-dev.yml
src/test/java/com/southstand/BackendConnectivityTests.java
src/test/java/com/southstand/card/FeedPerformanceStructureTests.java
scripts/windows/check-backend-connectivity.ps1
scripts/windows/benchmark-backend-api.ps1
scripts/windows/check-t14-network.ps1
docs/audits/T14_BACKEND_TIMEOUT_AUDIT.md
```

## 14. 修复前后性能

Feed 修复前冷请求 `2.6627s`，5 次热请求范围 `1.5446s` 到 `1.7788s`，单次 1406 SQL。修复后首次独立复验冷请求 `0.6308s`，5 次热请求范围 `0.0699s` 到 `0.0846s`；端到端新 JVM 复验冷请求 `0.2031s`，热请求平均 `0.0632s`。响应大小修复前后均为 `28,381 B`。

## 15. connectivity 脚本

`check-backend-connectivity.ps1` 已通过：确认 `[::]:8080`、PID/jar、127.0.0.1、localhost、WSL 地址和宿主机局域网地址均可达，并输出 `10.0.2.2` 指引。`adb` 不在 PATH 的事实会作为 INFO 显示，不伪造 reverse 结论。

## 16. benchmark 脚本

`benchmark-backend-api.ps1` 已通过。每个关键接口执行 1 次冷请求和 5 次热请求，输出 HTTP 状态、DNS、连接、首字节、总耗时、响应大小，以及热请求 min/avg/max。登录响应仅统计长度，不打印 Token。

## 17. check-t14-network 结果

脚本已完成 MySQL/Redis 检查、当前 jar 构建与启动、监听检查、connectivity、benchmark、T14 demo smoke、测试进程停止和端口残留检查，最终输出：

```text
T14 network check passed
```

## 18. 前端仍需修改

1. Base URL 设置为 `http://10.0.2.2:8080`，不要使用模拟器内的 localhost。
2. 确认 debug AndroidManifest 或开发专用 network security config 允许明文 HTTP。
3. 清除 App 数据后冷启动，记录实际请求 URL、DioException.type、connect/receive 阶段和耗时。
4. 开发环境可使用 `connectTimeout=10s`、`receiveTimeout=15s` 作为合理上限，但不能用提高超时掩盖错误地址。

## 19. 遗留问题

当前仓库不含 Flutter 源码，无法完成模拟器内 curl、Dio Base URL、cleartext 和 timeout 配置的直接复验；`adb` 也未加入 PATH。比赛列表、评论列表和混合搜索在日志中仍表现出 N+1 查询结构，但本次实测均远低于性能目标，后续应单独批量化并增加相应 Mapper 调用次数测试。HikariCP、Tomcat 和 Redis 没有显式 timeout/池参数，当前使用框架默认值；日志未发现连接获取超时、连接泄漏、通信失败或 Redis 连接失败，MySQL `Threads_connected=21`、`max_connections=151`，没有依据扩大连接池。

## 20. git status --short

工作区在本专项开始前已有 T14 数据、编码修复和文档改动。本次未执行 `git add`、`commit` 或 `push`。审计时状态摘要如下：

```text
 M README.md
 M docs/05_DATABASE_SCHEMA.md
 M docs/11_VALIDATION_AND_SMOKE_GUIDE.md
 M docs/12_CODEX_TASK_PLAN.md
 M scripts/sql/schema.sql
 M scripts/windows/reset-dev-db.ps1
 M scripts/windows/smoke-comment-hot.ps1
 M src/main/java/com/southstand/card/service/FeedService.java
 M src/main/java/com/southstand/common/config/SecurityConfig.java
 M src/main/resources/application-dev.yml
?? docs/audits/
?? scripts/data/
?? scripts/windows/benchmark-backend-api.ps1
?? scripts/windows/check-backend-connectivity.ps1
?? scripts/windows/check-t14-network.ps1
?? src/main/java/com/southstand/common/config/RequestTimingFilter.java
?? src/test/java/com/southstand/BackendConnectivityTests.java
?? src/test/java/com/southstand/card/FeedPerformanceStructureTests.java
```

## 最终结论

```text
后端是否正常启动：是
后端是否监听模拟器可访问地址：是，通配监听 [::]:8080，宿主机 IPv4 实测可达
宿主机接口是否超时：否
是否存在慢接口：修复前 Feed 明显偏慢；修复后所有基准接口达到目标
是否存在 N+1：Feed 曾存在且已修复；其他低耗时列表仍有结构优化空间
前端推荐 Base URL：http://10.0.2.2:8080
是否需要 adb reverse：否，推荐方案不依赖 adb reverse
是否已修复：后端 Feed 性能问题已修复；前端地址与 cleartext 仍需前端仓库复验
```
