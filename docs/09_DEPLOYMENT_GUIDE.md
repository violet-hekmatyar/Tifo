# 南看台构建与 Linux 部署计划

> 版本：v0.2-tifo-revised  
> 定位：本文档记录南看台后端计划采用的构建与部署方式。当前是部署方案模板，不代表已经完成真实部署。首次真实部署后，需要回填实际服务器路径、端口、镜像版本、问题记录和最终验收结果。

## 1. 目标部署模式

```text
Windows 本地开发和构建
-> GitHub 保存代码
-> Windows 本地构建 Docker 镜像
-> 导出镜像 tar 包
-> 上传 Linux 服务器
-> Linux 服务器 docker load
-> docker compose up -d 运行
```

## 2. 部署原则

服务器代理麻烦，因此不要让 Linux 服务器承担构建任务。

不推荐：

```text
Linux 服务器 git clone
Linux 服务器 mvn package
Linux 服务器 docker build
Linux 服务器从 Docker Hub 拉业务镜像
Linux 服务器下载 Maven 依赖
```

推荐：

```text
Windows 完成构建
Linux 只负责运行
```

### 2.1 当前阶段 jar 直跑方案

当前 T01 骨架阶段，后端优先采用 jar 直跑，不做后端 Docker 容器运行。

当前流程：

```text
Windows 本地开发
-> Windows 本地执行 mvn package 生成 jar
-> 上传 jar 到 Linux 后端运行目录
-> Linux 使用 java -jar 运行后端服务
```

T01 固定 jar 名称：

```text
south-stand-server.jar
```

T02 起后端已接入 MySQL / Redis，jar 启动前需要准备环境变量：

```text
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=south_stand
MYSQL_USERNAME=按服务器实际账号填写
MYSQL_PASSWORD=从服务器本地环境或 /opt/south-stand/.env 读取
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=如有密码则从本地环境读取
JWT_SECRET=从服务器本地环境或 /opt/south-stand/.env 读取
JWT_ACCESS_TOKEN_EXPIRE_SECONDS=604800
```

不要把真实密码写入 Git。

Linux 上 MySQL / Redis 已准备好，后端 jar 直跑时使用本机回环地址连接：

```text
MYSQL_HOST=127.0.0.1
REDIS_HOST=127.0.0.1
```

Linux jar 运行目录当前不写死真实路径。建议路径：

```text
/opt/south-stand/backend
```

实际路径以服务器人工确认为准。

示例运行命令：

```bash
cd /opt/south-stand/backend
java -jar south-stand-server.jar
```

如果需要指定环境变量，可使用：

```bash
MYSQL_HOST=127.0.0.1 REDIS_HOST=127.0.0.1 JWT_SECRET=change_me_to_long_random_secret_at_least_32_bytes java -jar south-stand-server.jar
```

Docker 镜像部署方案继续保留为后续可选方案。当前阶段只新增 jar 直跑路径，不删除原 Docker 方案。

## 3. 本地开发目录建议

```text
D:\south-stand-dev
├── south-stand-backend
├── docs
├── docker
└── scripts
```

## 4. GitHub 使用方式

```powershell
git status
git add .
git commit -m "docs: update backend document set from tifo requirements"
git push origin main
```

建议提交粒度：

```text
docs: update backend document set from tifo requirements
chore: init spring boot backend
feat: add auth onboarding flow
feat: add content card feed api
feat: add football data module
feat: add rating module placeholder
chore: add docker deployment files
```

## 5. Dockerfile 计划模板

项目根目录：

```text
south-stand-backend/Dockerfile
```

示例：

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/south-stand-server.jar /app/south-stand-server.jar

ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/south-stand-server.jar"]
```

第一版使用本地 Maven package 生成 jar，再 docker build。

## 6. 本地构建 jar

```powershell
cd D:\south-stand-dev\south-stand-backend
mvn clean package -DskipTests
```

产物示例：

```text
target/south-stand-server.jar
```

实际 jar 名称以后端项目配置为准。

## 7. 本地构建 Docker 镜像

```powershell
cd D:\south-stand-dev\south-stand-backend
docker build -t south-stand-backend:0.1.0 .
```

查看镜像：

```powershell
docker images | findstr south-stand-backend
```

## 8. 导出 Docker 镜像

```powershell
docker save -o south-stand-backend-0.1.0.tar south-stand-backend:0.1.0
```

## 9. 上传到 Linux 服务器

使用 scp：

```powershell
scp .\south-stand-backend-0.1.0.tar user@server_ip:/opt/south-stand/images/
```

也可以使用 Xftp、FinalShell、MobaXterm、WinSCP。

## 10. Linux 服务器目录建议

```text
/opt/south-stand
├── docker-compose.yml
├── .env
├── images
│   └── south-stand-backend-0.1.0.tar
├── mysql
│   └── data
├── redis
│   └── data
├── uploads
└── logs
```

创建目录：

```bash
sudo mkdir -p /opt/south-stand/{images,mysql/data,redis/data,uploads,logs}
sudo chown -R $USER:$USER /opt/south-stand
```

## 11. 导入镜像

```bash
cd /opt/south-stand/images
docker load -i south-stand-backend-0.1.0.tar
docker images | grep south-stand-backend
```

## 12. docker-compose.yml 计划模板

服务器目录：

```text
/opt/south-stand/docker-compose.yml
```

示例：

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: south-stand-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: south_stand
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - ./mysql/data:/var/lib/mysql
    command:
      --default-authentication-plugin=mysql_native_password
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7
    container_name: south-stand-redis
    restart: always
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - ./redis/data:/data

  backend:
    image: south-stand-backend:0.1.0
    container_name: south-stand-backend
    restart: always
    depends_on:
      - mysql
      - redis
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_DATABASE: south_stand
      MYSQL_USERNAME: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      JWT_SECRET: ${JWT_SECRET}
      UPLOAD_DIR: /app/uploads
    volumes:
      - ./uploads:/app/uploads
      - ./logs:/app/logs
```

如果服务器无法拉取 `mysql:8.0` 和 `redis:7`，就在 Windows 上提前 `docker save` 后上传导入。

## 13. .env 计划模板

服务器目录：

```text
/opt/south-stand/.env
```

示例：

```env
MYSQL_ROOT_PASSWORD=change_me_root
MYSQL_USER=southstand
MYSQL_PASSWORD=change_me_mysql
JWT_SECRET=change_me_to_long_random_secret
```

`.env` 不提交 GitHub。

## 14. 启动服务

```bash
cd /opt/south-stand
docker compose up -d
docker compose ps
docker logs -f south-stand-backend
```

## 15. 停止和重启

```bash
docker compose down
docker restart south-stand-backend
docker compose restart
```

## 16. 更新版本

Windows 构建新镜像：

```powershell
docker build -t south-stand-backend:0.1.1 .
docker save -o south-stand-backend-0.1.1.tar south-stand-backend:0.1.1
scp .\south-stand-backend-0.1.1.tar user@server_ip:/opt/south-stand/images/
```

Linux 导入并重启：

```bash
cd /opt/south-stand/images
docker load -i south-stand-backend-0.1.1.tar
cd /opt/south-stand
# 修改 docker-compose.yml 中 image 版本
docker compose up -d
```

## 17. 数据库初始化

建议提供：

```text
scripts/sql/schema.sql
scripts/sql/seed.sql
```

手动执行示例：

```bash
docker exec -i south-stand-mysql mysql -uroot -p south_stand < schema.sql
docker exec -i south-stand-mysql mysql -uroot -p south_stand < seed.sql
```

## 18. 常见问题预案

### 18.1 后端连不上 MySQL

如果采用当前阶段 jar 直跑，确认后端配置使用：

```text
MYSQL_HOST=127.0.0.1
REDIS_HOST=127.0.0.1
```

如果采用后续 Docker Compose 方案，确认后端配置使用容器名：

```text
MYSQL_HOST=mysql
```

Docker Compose 方案中不要使用：

```text
localhost
```

### 18.2 端口被占用

```bash
sudo lsof -i:8080
sudo lsof -i:3306
sudo lsof -i:6379
```

### 18.3 服务器拉不到 mysql/redis 镜像

Windows 执行：

```powershell
docker pull mysql:8.0
docker pull redis:7
docker save -o mysql-8.0.tar mysql:8.0
docker save -o redis-7.tar redis:7
```

Linux 执行：

```bash
docker load -i mysql-8.0.tar
docker load -i redis-7.tar
```

## 19. 首次真实部署后必须回填

| 项 | 实际值 |
|---|---|
| 服务器 IP / 域名 | 待补充 |
| 部署目录 | 待补充 |
| 后端端口 | 待补充 |
| MySQL 端口 | 待补充 |
| Redis 端口 | 待补充 |
| 镜像版本 | 待补充 |
| docker compose 文件路径 | 待补充 |
| 实际启动命令 | 待补充 |
| 遇到的问题 | 待补充 |
| 解决方案 | 待补充 |
| 验收结果 | 待补充 |

## 20. 部署验收计划

首次部署完成后验证：

```bash
curl http://server_ip:8080/api/public/health
curl http://server_ip:8080/api/public/health/db
curl http://server_ip:8080/api/public/health/redis
curl http://server_ip:8080/doc.html
curl http://server_ip:8080/api/app/feed
curl http://server_ip:8080/api/app/football/leagues
```

该章节在真实部署后补充最终结果。
