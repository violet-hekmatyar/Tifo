# 南看台后端文档地图

> 版本：v0.2-tifo-revised  
> 定位：后端开发文档集入口，用于说明文档清单、阅读顺序、边界和维护规则。本文只做导览，不承载具体实现细节。

## 1. 文档来源

当前文档集主要参考以下材料整理：

| 来源 | 用途 |
|---|---|
| `tifo需求文档.pdf` | 作为核心产品需求来源，补充首次登录、首页卡片、数据页、球队/球员/比赛详情、我的、消息等细节 |
| `南看台需求分析.pdf` | 作为产品方向、商业模式、功能特色、主队/数据/社区方向参考 |
| `3.18待确认问题.pdf` | 作为待确认问题来源，例如私信、详情页形式、战报赛况来源 |
| `技术栈选型与后端架构调整报告.pdf` | 作为后端技术路线与分阶段开发依据 |
| `环境与基础教程.pdf` | 作为版本环境基线 |
| 界面原型 | 作为页面结构、跳转关系和展示字段参考 |

后续如果原型图或需求发生调整，应优先更新 `02_REQUIREMENT_SCOPE.md`，再同步修改数据库、接口和测试文档。

## 2. 主线文档清单

| 文档 | 定位 | 主要读者 | 何时阅读 |
|---|---|---|---|
| `00_DOCUMENT_MAP.md` | 文档地图、阅读顺序、维护原则 | 所有人 / AI | 新成员接手、每轮开发前 |
| `01_PROJECT_OVERVIEW.md` | 项目定位、核心闭环、阶段目标 | 老板 / 产品 / 开发 | 了解项目整体方向 |
| `02_REQUIREMENT_SCOPE.md` | P0/P1/P2 功能范围、暂缓功能、待确认问题 | 产品 / 开发 / AI | 确认第一版做什么 |
| `03_TECH_STACK.md` | 技术栈、版本、工具、中间件边界 | 开发 | 建项目和配环境前 |
| `04_BACKEND_ARCHITECTURE.md` | 后端架构、模块划分、包结构 | 后端 / AI | 写业务代码前 |
| `05_DATABASE_SCHEMA.md` | 表结构、字段、索引、枚举、样例数据 | 后端 / AI | 写 Entity/Mapper/Service 前 |
| `06_API_SPEC.md` | App / 管理后台接口规范、统一返回、分页、错误码 | 前后端 / AI | 写 Controller 和联调前 |
| `07_AUTH_SECURITY.md` | 登录、JWT、密码加密、基础权限、安全底线 | 后端 / AI | 写登录和权限前 |
| `08_ALGORITHM_INTEGRATION.md` | 推荐算法槽位、规则推荐、降级策略 | 算法 / 后端 | 对接推荐能力前 |
| `09_DEPLOYMENT_GUIDE.md` | Docker 镜像构建与 Linux 部署计划模板 | 后端 / 运维 | 首次部署前后 |
| `10_AI_CODING_RULES.md` | Cursor/Codex/Copilot 辅助开发规范 | 开发 / AI | 每轮 AI Coding 前 |
| `11_VALIDATION_AND_SMOKE_GUIDE.md` | 单元测试、接口测试、场景测试与验收计划 | 开发 / 测试 | 提交、联调、部署前 |
| `12_CODEX_TASK_PLAN.md` | Codex 大任务路线图、任务边界和验收格式 | 开发 / AI | 每轮 Codex 任务拆分前 |

## 3. 推荐阅读路径

### 3.1 快速理解项目

```text
00_DOCUMENT_MAP
-> 01_PROJECT_OVERVIEW
-> 02_REQUIREMENT_SCOPE
```

### 3.2 开始后端开发

```text
02_REQUIREMENT_SCOPE
-> 03_TECH_STACK
-> 04_BACKEND_ARCHITECTURE
-> 05_DATABASE_SCHEMA
-> 06_API_SPEC
-> 07_AUTH_SECURITY
```

### 3.3 使用 Cursor / Codex 开发

```text
00_DOCUMENT_MAP
-> 12_CODEX_TASK_PLAN
-> 10_AI_CODING_RULES
-> 02_REQUIREMENT_SCOPE
-> 04_BACKEND_ARCHITECTURE
-> 05_DATABASE_SCHEMA
-> 06_API_SPEC
-> 11_VALIDATION_AND_SMOKE_GUIDE
```

### 3.4 部署与验收

```text
09_DEPLOYMENT_GUIDE
-> 11_VALIDATION_AND_SMOKE_GUIDE
```

### 3.5 原有 AI 开发最小阅读路径

```text
10_AI_CODING_RULES
-> 02_REQUIREMENT_SCOPE
-> 04_BACKEND_ARCHITECTURE
-> 05_DATABASE_SCHEMA
-> 06_API_SPEC
-> 11_VALIDATION_AND_SMOKE_GUIDE
```

## 4. 当前阶段

当前项目处于“仓库基础准备 + 后端快速搭架子前置确认”阶段。

优先事项：

```text
仓库基础整理
忽略规则
部署路径文档
Codex 任务边界
仓库检查脚本
项目骨架
数据库设计
登录鉴权
首次登录偏好设置
首页卡片流
内容/评论/点赞/收藏
足球基础数据
球队/球员/比赛详情基础接口
我的页面
管理后台基础接口
测试与验证计划
```

暂不优先：

```text
完整私信 IM
WebSocket 实时比分
复杂推荐算法
第三方数据 API 正式接入
Elasticsearch
Kafka
ClickHouse
Kubernetes
复杂微服务拆分
完整广告系统
会员/支付
```

## 5. 文档维护原则

1. 需求变更先改 `02_REQUIREMENT_SCOPE.md`。
2. 表结构变化必须同步更新 `05_DATABASE_SCHEMA.md`、`schema.sql`、`seed.sql`。
3. API 变化必须同步更新 `06_API_SPEC.md` 和接口测试用例。
4. 安全策略变化必须同步更新 `07_AUTH_SECURITY.md`。
5. 推荐规则变化必须同步更新 `08_ALGORITHM_INTEGRATION.md`。
6. 部署实际结果必须回填到 `09_DEPLOYMENT_GUIDE.md`。
7. 测试场景变化必须同步更新 `11_VALIDATION_AND_SMOKE_GUIDE.md`。
8. AI Coding 前必须明确读取哪些文档、允许修改哪些文件、如何验收。
9. 原型图不能直接作为 AI Coding 的唯一输入，必须先转成文字需求。
10. 旧方案不混入主线文档，可放入 `docs/archive/`。
11. Codex 大任务顺序和边界变化时，必须同步更新 `12_CODEX_TASK_PLAN.md`。
