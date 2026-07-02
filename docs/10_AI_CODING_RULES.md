# 南看台 AI Coding 开发规范

> 版本：v0.2-tifo-revised  
> 定位：定义南看台项目使用 Cursor / Codex / Copilot 等 AI Coding 工具时的开发规范。

## 1. 核心目标

```text
让 AI 成为受控的开发助手，而不是让 AI 自由决定项目架构。
```

## 2. 核心原则

### 2.1 人负责设计，AI 负责辅助实现

人负责：

```text
产品边界
技术栈
数据库设计
接口规范
安全方案
部署方案
模块边界
最终代码合并
```

AI 负责：

```text
生成样板代码
补充 DTO / VO / Entity
实现 Controller / Service / Mapper
修复编译错误
生成测试脚本
整理局部文档
解释报错
局部重构
```

### 2.2 先定文字需求，再写代码

禁止直接让 AI 根据原型截图自由猜需求。

开发前必须明确：

```text
文字版需求
数据库 Schema
API Spec
Auth Security
Backend Architecture
验收方式
```

如果需求只存在图片原型中，必须先整理成文字版需求，再让 AI 开发。

### 2.3 先定契约，再写代码

未确定契约前，不允许让 AI 直接生成大范围业务代码。

契约包括：

```text
数据库字段
接口路径
统一返回结构
权限要求
错误码
测试场景
```

### 2.4 按功能闭环推进

推荐按闭环推进：

```text
用户登录闭环
首次登录偏好闭环
首页卡片流闭环
内容详情 + 评论闭环
球队详情闭环
球员详情闭环
比赛详情闭环
我的页面闭环
管理后台内容发布闭环
Docker 部署闭环
```

每个闭环都必须能运行、能测试、能提交。

## 3. 推荐工具分工

| 工具 | 用途 |
|---|---|
| ChatGPT | 需求拆解、架构讨论、文档规划、错误分析 |
| Cursor | 项目内多文件修改、代码生成、重构 |
| Codex CLI | 命令行辅助开发、修复错误、生成测试 |
| GitHub Copilot | IDE 内代码补全 |
| IDEA | Java 项目主开发、运行、调试、Review |
| Apifox | 接口测试 |
| PowerShell | 本地脚本执行 |

## 4. AI 开发上下文包

每次让 AI 开发前，必须准备 Context Pack：

```text
当前目标
项目背景
相关文档
允许修改范围
禁止修改范围
实现要求
验收方式
输出要求
```

相关文档选择：

| 任务 | 应提供文档 |
|---|---|
| 项目初始化 | 03、04、09、10 |
| 数据库开发 | 02、05、10 |
| 接口开发 | 02、05、06、07、10 |
| 登录鉴权 | 05、06、07、10 |
| 首页卡片流 | 02、04、05、06、08、10 |
| 球队/球员/比赛详情 | 02、04、05、06、10 |
| 推荐模块 | 08、05、06、10 |
| 部署 | 03、09、11 |
| 测试脚本 | 06、09、11 |

### 4.1 当前项目执行规则

当前项目每一轮 Codex 任务必须遵守：

```text
每轮任务必须有明确边界，不能自动扩大范围
每轮必须提供并运行对应测试脚本
测试不通过不能算完成
不允许提交敏感配置
不允许自动 push 到远程仓库
不允许在未授权情况下 git add / git commit
```

如果本轮没有 Maven / Spring Boot 项目，不要求执行 `mvn test` 或 `mvn package`，但必须提供与本轮目标匹配的检查脚本。

从 T01 开始，每轮应优先提供并执行 `scripts/windows/check-*.ps1`。T01 骨架验收脚本为：

```powershell
.\scripts\windows\check-t01.ps1
```

该脚本必须覆盖编译、测试、打包、jar 启动和 health smoke。

从 T02 开始，如本轮引入数据库、中间件或外部依赖，检查脚本还必须覆盖连接健康检查、初始化脚本和最小 smoke 流程。

从 T03 开始，如本轮涉及鉴权，检查脚本必须覆盖注册、登录、当前用户、无 Token、普通用户访问后台、管理员访问后台和登录失败防刷。

从 T04 开始，关注球队不设置数量上限；检查脚本应验证首次登录偏好、关注/取消关注 toggle、当前用户资料和无 Token 场景。

## 5. 标准 Prompt 模板

```text
当前目标：
说明这次要完成的功能闭环或局部能力。

项目背景：
南看台是卡片化足球内容流 + 赛事数据 + 社区互动 APP，当前后端采用 Spring Boot + MySQL + Redis。

相关文档：
- docs/02_REQUIREMENT_SCOPE.md
- docs/04_BACKEND_ARCHITECTURE.md
- docs/05_DATABASE_SCHEMA.md
- docs/06_API_SPEC.md
- docs/10_AI_CODING_RULES.md

允许修改：
明确允许修改的目录或文件。

禁止修改：
明确禁止修改的目录或文件。

实现要求：
说明必须遵守的返回结构、异常处理、权限、命名规范。

验收方式：
说明需要运行哪些命令或调用哪些接口。

输出要求：
列出修改文件、实现说明、测试命令、风险点。
```

## 6. 后端开发 Prompt 示例

```text
当前目标：
实现首页卡片流 P0 接口，支持 recommend/news/following/team 四个 tab，至少返回 CONTENT_CARD 和 MATCH_CARD。

项目背景：
南看台后端采用 Spring Boot 3.2.4、MySQL、Redis、MyBatis-Plus、JWT、Knife4j。

相关文档：
- docs/02_REQUIREMENT_SCOPE.md
- docs/04_BACKEND_ARCHITECTURE.md
- docs/05_DATABASE_SCHEMA.md
- docs/06_API_SPEC.md
- docs/08_ALGORITHM_INTEGRATION.md
- docs/10_AI_CODING_RULES.md

允许修改：
- src/main/java/com/southstand/card/**
- src/main/java/com/southstand/recommend/**
- src/main/java/com/southstand/content/** 中必要查询类
- src/main/java/com/southstand/football/** 中必要查询类

禁止修改：
- 技术栈
- 数据库表结构
- 统一返回结构
- JWT 鉴权方案
- Docker 配置
- .env

实现要求：
- Controller 不直接返回 Entity
- Controller 不写业务逻辑
- 使用 DTO / VO
- 使用统一 Result
- 异常使用全局异常处理
- 不新增未确认依赖

验收方式：
- mvn test 通过
- mvn package 通过
- Knife4j 能看到接口
- /api/app/feed?tab=recommend 能返回 seed 数据

输出要求：
- 列出修改文件
- 说明实现逻辑
- 给出 curl 或 Apifox 测试方式
```

## 7. 禁止开放式 Prompt

禁止：

```text
帮我把整个后端写完
帮我生成一个足球 APP 后端
随便优化一下项目
把所有接口都实现
你看着改
根据这个原型图直接做
```

## 8. AI 禁止事项

AI 不允许：

```text
自行更换技术栈
自行引入 Elasticsearch / Kafka / ClickHouse / Kubernetes
自行新增大型依赖
自行修改数据库表结构而不说明
自行修改统一返回格式
自行修改 API 路径
直接返回 Entity 给前端
在 Controller 写复杂业务逻辑
把密码、Token、密钥写入代码
提交 .env
删除已有文档
一次性重写整个项目
绕过鉴权直接开放后台接口
根据截图猜字段和接口
把 P2 功能当成 P0 开发
自动扩大本轮任务范围
自动提交敏感配置
自动 push 到远程仓库
```

## 9. 文档同步规则

每次改数据库，必须同步修改：

```text
docs/05_DATABASE_SCHEMA.md
scripts/sql/schema.sql
scripts/sql/seed.sql
```

每次改接口，必须同步修改：

```text
docs/06_API_SPEC.md
Apifox / Knife4j 说明
测试脚本
```

每次改需求边界，必须同步修改：

```text
docs/02_REQUIREMENT_SCOPE.md
相关数据库/接口/测试文档
```

## 10. 代码审查清单

基础审查：

```text
是否只修改允许范围
是否新增未确认依赖
是否有未使用 import
是否有明显重复代码
是否破坏包结构
是否能编译
```

契约审查：

```text
是否符合数据库字段
是否符合 API 返回结构
是否符合分页结构
是否符合错误码
是否符合 JWT 鉴权
是否符合命名规范
```

安全审查：

```text
是否泄露密码
是否打印完整 Token
是否提交 .env
是否绕过权限判断
是否允许普通用户访问后台接口
是否上传危险文件
```

## 11. 功能闭环完成标准

```text
代码能编译
服务能启动
接口能调用
成功场景可验证
失败场景有错误提示
数据库数据正确变化
Knife4j 文档可见
Git diff 已 Review
有清晰 commit message
```

## 12. 推荐开发顺序

```text
1. 初始化 Spring Boot 项目
2. 配置 MySQL / Redis / MyBatis-Plus / Knife4j
3. 实现 common：Result、PageResult、异常、错误码
4. 实现 auth：注册、登录、JWT、当前用户
5. 实现 onboarding：主队、关注球队、关注球员
6. 实现 user：用户资料、我的页面基础
7. 实现 football 基础：联赛、球队、球员、比赛
8. 实现 content：首页资讯卡片、详情、后台发布
9. 实现 interaction：评论、二级回复、点赞、收藏
10. 实现 follow：关注球队、球员、用户、粉丝状态
11. 实现 card/recommend：首页多卡片基础规则
12. 实现 match detail：总览、事件、战报、统计占位
13. 实现 rating：球员评分 P1
14. 实现 message：系统和互动消息
15. 实现 Docker 部署
16. 实现测试脚本
17. P2 再考虑 pickup、私信、转会中心、复杂推荐
```

## 13. Git 留痕规范

提交信息建议：

```text
docs: update backend docs from tifo requirements
chore: init spring boot project
feat: add auth login jwt flow
feat: add onboarding preferences flow
feat: add content card feed api
feat: add football team player match api
feat: add interaction comment like favorite
feat: add match rating module
feat: add docker deployment files
test: add backend smoke script
fix: fix jwt filter unauthorized response
```

每次提交前：

```powershell
git status
mvn test
mvn package
```

如果当前轮次尚未创建 Maven 项目，则以本轮检查脚本替代 Maven 命令，例如：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows\check-repo.ps1
```
