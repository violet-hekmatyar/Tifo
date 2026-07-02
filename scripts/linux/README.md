# Linux 脚本说明

当前 T01 只提供 jar 直跑脚本模板，不写真实服务器密码、Token 或生产密钥。

后续 T10/T12 可继续完善正式部署和回滚脚本：

```text
run-backend.sh
stop-backend.sh
check-backend.sh
```

当前阶段 Linux 后端优先采用 jar 直跑。建议 jar 路径：

```text
/opt/south-stand/backend/south-stand-server.jar
```

建议日志目录：

```text
/opt/south-stand/logs/backend
```

Linux 直跑时，MySQL / Redis 建议通过本机回环地址连接：

```text
MYSQL_HOST=127.0.0.1
REDIS_HOST=127.0.0.1
```

真实环境变量应从 Linux 本地 `.env` 或 shell 环境读取，不提交到 Git。

T02 起后端会连接 MySQL / Redis。Linux jar 直跑时建议确认：

```text
MYSQL_HOST=127.0.0.1
REDIS_HOST=127.0.0.1
MYSQL_PASSWORD、REDIS_PASSWORD 从本地环境或 /opt/south-stand/.env 读取
```

健康检查：

```text
/api/public/health
/api/public/health/db
/api/public/health/redis
```
