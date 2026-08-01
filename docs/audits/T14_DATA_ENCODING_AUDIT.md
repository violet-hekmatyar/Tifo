# T14 演示数据编码专项审计

审计日期：2026-08-01  
审计范围：`D:\Football-APP` 本地开发环境，不涉及生产数据库。

## 结论

```text
数据库是否有问题：是（修复前 T14 中文值已被存成问号；库表字符集本身正常）
后端 API 是否有问题：否（修复前忠实返回数据库问号，重新导入后返回正常 UTF-8 中文）
前端是否有问题：暂无法确认（当前工作区没有 Flutter/Dart 前端源码）
当前问号根因：B. Windows 导入阶段乱码
是否已修复：是（导入链路和 dev 数据库已修复；前端 UI 仍需清缓存后人工复验）
是否可以继续使用 T14 数据：是
```

## 1. 现象分析

已知页面静态中文正常，而动态标题、昵称、球队和评论显示大量 `????`。这首先排除了“整个字体不支持中文”，但不能直接区分数据库、HTTP 或前端缓存问题。本次没有收到可读取的截图文件，分析基于任务描述和真实链路取证。

## 2. 前端 Base URL

当前仓库未发现 `.dart`、`pubspec.yaml`、Flutter API Client、Dio 配置或前端环境文件，因此无法证明正在运行的前端实际 Base URL。后端接口文档记录的开发地址为 `http://localhost:8080`，本次原始 HTTP 取证也使用该地址。前端实际地址仍需在前端仓库或运行配置中核对。

## 3. 后端与数据库目标

```text
后端 profile：dev
MySQL host：localhost（配置默认值）
MySQL port：3306
database：south_stand
MySQL 容器：apihub-mysql / mysql:8.0
后端审计端口：8080
审计结束后的后端 PID：无
8080 / 8090 监听：无
```

`application.yml`、`application-dev.yml` 和 T14 初始化脚本均默认连接上述目标。未输出密码、JWT 或 Token。

## 4. seed-demo.sql 文件证据

对 `scripts/sql/seed-demo.sql` 原始字节执行 Python 检查：

```text
UTF-8 解码：通过
BOM：无
文件大小：604309 bytes
中文字符数：33975
ASCII 问号数：0
连续问号段数：0
SHA-256：141bdadfc2c70637a81c665946743b2d92a86230ea55e9503f00b4f651b537d3
```

生成器、`demo-names.json` 和 SQL 文件中均保留真实中文，因此排除 A（SQL 生成阶段乱码）。

## 5. MySQL 字符集与排序规则

修复前后字符集配置一致：

```text
character_set_client=utf8mb4
character_set_connection=utf8mb4
character_set_results=utf8mb4
character_set_database=utf8mb4
collation_connection=utf8mb4_0900_ai_ci
collation_database=utf8mb4_unicode_ci
```

数据库、`football_league`、`football_team`、`football_player`、`user_profile`、`content`、`comment` 及其文本列均为 `utf8mb4 / utf8mb4_unicode_ci`，未发现 latin1 或 ascii 列。因此排除 C（库表字符集错误）。

## 6. 数据库存储与 HEX 证据

修复前：

```text
球队样例：???，HEX=3F3F3F
球员样例：???，HEX=3F3F3F
用户昵称样例：??????，HEX=3F3F3F3F3F3F
内容标题：连续问号，HEX 全为 3F
评论正文：连续问号，HEX 全为 3F
```

修复并重新导入后：

```text
球队样例：阿森纳
HEX：E998BFE6A3AEE7BAB3
内容标题样例：阿森纳赛前训练完成，年轻球员进入大名单
HEX 起始：E998BFE6A3AEE7BAB3E8B59B...
```

## 7. 连续问号统计

| 数据 | 修复前中文正常 / 总数 | 修复前连续问号 | 修复后中文正常 / 总数 | 修复后连续问号 |
| --- | ---: | ---: | ---: | ---: |
| 联赛 | 未单独统计 | 已受影响 | 8 / 8 | 0 |
| 球队 | 0 / 24 | 24 | 24 / 24 | 0 |
| 球员 | 0 / 144 | 144 | 144 / 144 | 0 |
| 用户昵称 | 0 / 36 | 36 | 36 / 36 | 0 |
| 内容标题/正文 | 0 / 120 | 120 | 120 / 120 | 0 |
| 评论 | 0 / 520 | 520 | 520 / 520 | 0 |

## 8. Windows 导入根因

修复前 `reset-dev-db.ps1` 和 `init-demo-data.ps1` 使用：

```powershell
Get-Content -Raw -Encoding UTF8 ... | mysql
Get-Content -Raw -Encoding UTF8 ... | docker exec -i ... mysql
```

审计环境为 PowerShell 5.1、活动代码页 936、`$OutputEncoding=us-ascii`。PowerShell 将已解码的 Unicode 字符串写入 native executable 管道时按 ASCII 编码，无法表示的中文被替换为 `?`。脚本也未显式传递 `--default-character-set=utf8mb4`。SQL 文件正常而数据库 HEX 为 `3F`，与该路径完全对应，因此根因确定为 B。

修复后新增 `mysql-file-utils.ps1`：本机客户端通过 `RedirectStandardInput` 读取 SQL 原始字节；Docker 模式通过 `docker cp` 二进制复制后在容器内重定向；两条路径都显式使用 `--default-character-set=utf8mb4`。

## 9. Mapper / Service 结果

`DemoDataEncodingTests` 直接通过 Mapper 查询固定 T14 ID，并断言：

- 球队、球员、昵称、内容标题、正文、评论包含中文；
- 字符串不是全问号且不包含连续问号；
- Feed 内容卡标题包含中文；
- 使用“阿森纳”和“林远航”进行实体搜索可以命中固定球队、球员。

真实 dev 数据库执行结果：2 项测试，0 失败、0 错误、0 跳过。

## 10. HTTP 原始 JSON

原始响应由 `curl.exe` 直接保存为字节文件，再由 Python 以 UTF-8 解码并解析 JSON，而不是依赖 PowerShell 控制台显示。

修复前：

```text
feed.json：中文 0，ASCII 问号 1863
news.json：中文 0，ASCII 问号 1841
teams.json：中文 0，ASCII 问号 128
players.json：中文 0，ASCII 问号 134
profile.json：中文 0，ASCII 问号 24
```

修复后：

```text
feed.json：中文 1767，连续问号 0
news.json：中文 1744，连续问号 0
teams.json：中文 134，连续问号 0
players.json：中文 146，连续问号 0
profile.json：中文 22，连续问号 0
```

## 11. Content-Type 与后端编码

所有抽查接口均返回 `Content-Type: application/json`，原始 body 可严格按 UTF-8 解码并通过 JSON 解析。项目未发现自定义 `HttpMessageConverter`、编码 Filter、`ResponseBodyAdvice` 或代理转码。数据库修复后无需修改 Jackson 或 HTTP 编码配置，排除 D 和 E。

## 12. 前端网络层检查

当前工作区没有前端源码，无法检查 Dio transformer、`utf8.decode`、`latin1.decode`、`bodyBytes` 或 DTO 反序列化。后端修复后的原始 JSON 已证明是正常 UTF-8 中文；若 UI 仍显示问号，应在前端仓库检查实际 Base URL、响应字节解码和缓存数据源。

## 13. 字段映射、字体、fallback 与缓存

仓库内未发现 Flutter mock、fixture、Hive、SharedPreferences、SQLite 或问号 fallback。静态中文可正常显示，字体不是首要问题。前端仍需确认动态卡片是否使用不同字体，以及是否缓存了修复前 API 返回的问号字符串。

推荐前端复验顺序：

1. 确认 Base URL 指向当前 `localhost:8080` 对应开发后端（模拟器按平台使用正确宿主地址）；
2. 停止旧后端并使用当前 jar 重启；
3. 清除 App 数据、Hive/SQLite/SharedPreferences 和 HTTP 缓存，必要时卸载开发包；
4. 冷启动，不使用热重载保留的旧状态；
5. 在网络面板比对标题、摘要、昵称、球队名与 `tmp/encoding-audit/*.json`。

## 14. 根因分类

确认根因：**B. Windows 导入阶段乱码**。

已排除：A、C、D、E。  
F、G 未发现证据且前端源码不可见。  
H 可能影响修复后的页面复验，但不是数据库原始问号的形成原因。

## 15. 修改文件

```text
scripts/windows/mysql-file-utils.ps1
scripts/windows/reset-dev-db.ps1
scripts/windows/init-demo-data.ps1
scripts/sql/check-demo-encoding.sql
scripts/windows/check-demo-encoding.ps1
scripts/data/check-demo-encoding.py
src/test/java/com/southstand/DemoDataEncodingTests.java
docs/audits/T14_DATA_ENCODING_AUDIT.md
```

未修改业务 Service、Jackson 配置或前端代码，没有用硬编码 UPDATE 猜测原文。

## 16. dev 数据重新导入

已确认目标为本地 `south_stand` dev 数据库后执行 `init-demo-data.ps1 -SkipGenerate`。脚本重置 dev 库、导入基础 seed、按原始字节导入同一份确定性 `seed-demo.sql`，随后 `validate-demo-data.sql` 全部通过。

## 17. 编码专项测试

```text
check-demo-encoding.ps1：通过
SQL 七组编码检查：全部 anomaly_count=0
HTTP 五组原始响应：全部通过
DemoDataEncodingTests（真实 dev DB）：2/2 通过
```

未配置 `MYSQL_PASSWORD` 的普通单测环境会明确跳过两项 dev 集成断言；`check-t14.ps1` 配置本地容器连接后会完整执行。

## 18. Maven 与 T14 回归

```text
mvn test（普通环境）：54 项，0 失败，0 错误，2 项 dev 集成测试跳过
check-t14.ps1 内 mvn test：54 项，0 失败，0 错误，0 跳过
check-t14.ps1：通过
T03-T13 Smoke：通过
T14 demo data Smoke：通过
```

## 19. 前端重新验证

未完成 UI 人工验证，因为当前工作区没有 Flutter/Dart 源码或可运行前端。后端 API 原始字节已经恢复正常，前端需按第 13 节清缓存并重新进入首页确认显示。

## 20. 遗留问题

- 无后端编码遗留问题；
- 前端真实 Base URL、缓存实现和最终 UI 显示尚待在前端工程中确认；
- `application*.yml` 当前使用 `characterEncoding=utf8`，真实 Mapper 测试已证明本项目中文链路正常，因此本次不做无证据配置修改。

## 21. Git 状态

审计结束时工作区仍包含此前未提交的 T14 文件和本次编码修复文件。未执行 `git add`、`commit` 或 `push`，未跟踪 `.env`、生产密钥、`target`、`tmp` 或上传目录。

```text
 M README.md
 M docs/05_DATABASE_SCHEMA.md
 M docs/11_VALIDATION_AND_SMOKE_GUIDE.md
 M docs/12_CODEX_TASK_PLAN.md
 M scripts/sql/schema.sql
 M scripts/windows/reset-dev-db.ps1
 M scripts/windows/smoke-comment-hot.ps1
 M src/main/java/com/southstand/common/config/SecurityConfig.java
?? docs/audits/
?? scripts/data/
?? scripts/sql/check-demo-encoding.sql
?? scripts/sql/seed-demo.sql
?? scripts/sql/validate-demo-data.sql
?? scripts/windows/check-demo-encoding.ps1
?? scripts/windows/check-t14.ps1
?? scripts/windows/init-demo-data.ps1
?? scripts/windows/mysql-file-utils.ps1
?? scripts/windows/smoke-demo-data.ps1
?? src/main/resources/static/
?? src/test/java/com/southstand/DemoDataEncodingTests.java
```
