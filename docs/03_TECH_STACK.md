# 南看台技术栈与版本选型

> 版本：v0.2-tifo-revised  
> 定位：记录南看台第一版采用的技术栈和版本，不记录复杂历史讨论。用于约束项目初始化、Maven 依赖、Docker 镜像、AI Coding 技术边界和本地环境配置。

## 1. 总体技术路线

第一版采用：

```text
Java 后端
+ Spring Boot
+ MySQL
+ Redis
+ MyBatis-Plus
+ Spring Security / JWT
+ Knife4j
+ Docker Compose
```

开发策略：

```text
先单体模块化，后期可演进为轻量 Spring Cloud。
```

第一版不引入：

```text
Elasticsearch
Kafka
ClickHouse
Kubernetes
复杂微服务拆分
Service Mesh
复杂 WebSocket 实时服务
```

## 2. 后端核心版本

| 技术 | 版本 | 说明 |
|---|---:|---|
| JDK | 17 LTS | Spring Boot 3 推荐基线 |
| Spring Boot | 3.2.4 | 后端核心框架 |
| Spring Cloud | 2023.0.1 | Gateway / 后期微服务扩展 |
| Spring Cloud Alibaba | 2023.0.1.0 | Nacos 适配 |
| Nacos Server | 2.3.2 | 可选，服务注册与配置管理 |
| Maven | 3.9.9 | 项目构建 |
| MySQL | 8.0.36 | 主业务数据库 |
| Redis | 7.2.5 | 缓存、登录状态、热点计数、简单限流 |
| MyBatis-Plus | 3.5.7 | ORM / CRUD 简化 |
| Knife4j | 4.5.0 | Swagger 接口文档增强 |
| Lombok | 1.18.32 | 简化 Java 样板代码 |
| JWT | jjwt 0.12.5 | 登录认证 Token |
| Docker Desktop | 4.x | 本地镜像构建和容器运行 |

## 3. 开发工具

| 工具 | 用途 | 说明 |
|---|---|---|
| Windows | 主开发环境 | 当前开发者主要在 Windows 上开发 |
| IntelliJ IDEA | Java 后端开发 | Community / Ultimate 均可 |
| Cursor | AI Coding 主力候选 | 适合多文件编辑、全栈开发 |
| Codex / Copilot | AI Coding 辅助 | 可作为 Cursor 之外的备选 |
| Git | 代码版本管理 | 代码托管到 GitHub |
| GitHub | 远程仓库 | 保存代码和提交历史 |
| Apifox | 接口测试 | 调试 App 端和后台接口 |
| DataGrip | 数据库管理 | 可选 |
| Docker Desktop | 本地构建镜像 | 用于构建后上传服务器 |
| PowerShell | 脚本执行 | Windows 本地 smoke 脚本 |

## 4. 后端依赖范围

### 4.1 P0 依赖

```text
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-security
spring-boot-starter-data-redis
mysql-connector-j
mybatis-plus-boot-starter
knife4j-openapi3-jakarta-spring-boot-starter
jjwt-api / jjwt-impl / jjwt-jackson
lombok
```

### 4.2 P1 可选依赖

```text
spring-cloud-starter-gateway
spring-cloud-starter-alibaba-nacos-discovery
spring-cloud-starter-alibaba-nacos-config
minio
```

### 4.3 暂不引入

```text
Elasticsearch Client
Kafka Client
ClickHouse Driver
RocketMQ
ShardingSphere
OAuth2 Authorization Server
复杂 WebSocket 封装
```

## 5. 数据与媒体处理边界

| 类型 | 第一版方案 | 后期方案 |
|---|---|---|
| 图片 | 本地 uploads / Docker volume 存储 URL | MinIO / OSS |
| 视频 | 不存储视频文件，仅保留外部链接占位 | 合作平台嵌入 / 对象存储 |
| GIF 集锦 | 不强依赖，避免版权和数据成本 | 数据 API / 合规采集 |
| 比分赛程 | 后台录入 + seed 数据 | 第三方体育数据 API |
| 球员/球队数据 | 后台录入 + seed 数据 | 第三方 API / 半自动导入 |
| 搜索 | MySQL 简单查询 | Elasticsearch |
| 推荐 | MySQL + Redis 规则排序 | Python 推荐服务 |

## 6. 中间件使用边界

| 中间件 | 第一版用途 | 后期用途 |
|---|---|---|
| MySQL | 用户、内容、互动、足球数据、评分、后台管理 | 主业务库 |
| Redis | 登录缓存、热点计数、简单限流、排行榜缓存 | 缓存穿透保护、推荐缓存 |
| Nacos | 可选，本地可先不用 | 后期微服务注册和配置 |
| Docker Compose | 本地/服务器运行 | 后期可替换为更复杂部署 |
| 本地文件系统 | 图片/文件存储占位 | 后期可换 MinIO / OSS |

## 7. 前端与后台建议

| 端 | 建议技术 |
|---|---|
| 移动端 APP | Flutter + Dart |
| 管理后台 | React + Ant Design |
| 接口测试 | Apifox |
| API 文档 | Knife4j |

移动端 APP：Flutter + Dart
状态管理：Riverpod
路由：go_router
网络请求：dio
H5：仅作为分享页 / 临时演示 / 后期补充，不作为主端

## 8. 版本锁定原则

1. 第一版不得随意升级 Spring Boot 主版本。
2. 不得自行引入大型中间件。
3. 不得为了单个功能引入复杂框架。
4. 新增依赖必须写明用途，并同步更新本文档。
5. AI Coding 不允许自行改变技术栈。
6. 能用 MySQL + Redis 解决的第一版需求，不引入 Elasticsearch / Kafka / ClickHouse。
7. 比赛实时性需求第一版通过刷新和排序处理，不做 WebSocket 实时比分。

## 9. 推荐工程结构

第一版建议单模块开发，包内模块化：

```text
south-stand-backend
├── pom.xml
├── Dockerfile
├── src/main/java/com/southstand
├── src/main/resources
└── scripts
```

后续如果需要再拆多模块。
