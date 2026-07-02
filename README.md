# 南看台 / Tifo

南看台 / Tifo 是一个卡片化足球内容流 + 赛事数据 + 社区互动 APP。

当前阶段：T01 Spring Boot 后端骨架与基础配置，已具备最小编译、打包、启动和健康检查闭环。

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
