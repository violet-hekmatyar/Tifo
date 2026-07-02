# 南看台 / Tifo

南看台 / Tifo 是一个卡片化足球内容流 + 赛事数据 + 社区互动 APP。

当前阶段：后端 P0 可演示闭环开发前准备，先稳定仓库、文档、忽略规则和检查脚本。

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
