# spring-java 本地运行说明

本文档用于在新环境中启动 MySQL、Redis 和 Spring Boot 应用。以下命令默认在 `spring-java` 目录下执行。

## 环境要求

- JDK 17 或更高版本
- Docker
- 本机端口 `3306`、`6379` 和 `8080` 未被占用

项目已提供 Maven Wrapper，不需要单独安装 Maven。

## 1. 启动 MySQL

```bash
docker run --name hard-mysql \
  -e MYSQL_ROOT_PASSWORD=hard-local-password \
  -e MYSQL_DATABASE=spring \
  -p 3306:3306 \
  -d mysql:8.4 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci
```

首次启动需要等待 MySQL 完成初始化。可以通过日志确认启动状态：

```bash
docker logs -f hard-mysql
```

看到 `ready for connections` 后按 `Ctrl+C` 退出日志查看。

## 2. 初始化数据库

执行建表脚本：

```bash
docker exec -e MYSQL_PWD=hard-local-password -i hard-mysql \
  mysql -uroot < db/create.sql
```

`db/create.sql` 会创建并使用 `spring` 数据库，且支持重复执行。新环境只需要执行该文件；`db/update.sql` 用于历史数据库升级，不需要在全新数据库上执行。

## 3. 启动 Redis

```bash
docker run --name hard-redis \
  -p 6379:6379 \
  -d redis:7-alpine
```

确认 Redis 可用：

```bash
docker exec hard-redis redis-cli ping
```

返回 `PONG` 表示启动成功。

## 4. 启动应用

项目默认启用 `dev` 配置。通过环境变量覆盖本地密码和 JWT 密钥后启动：

```bash
SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/spring?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai' \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD=hard-local-password \
SPRING_DATA_REDIS_HOST=127.0.0.1 \
SPRING_DATA_REDIS_PORT=6379 \
APP_JWT_SECRET='0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef' \
APP_LEGACY_CONTROLLERS_ACCOUNT_ENABLED=true \
./mvnw spring-boot:run
```

应用默认监听 `8080` 端口。上面的 `APP_LEGACY_CONTROLLERS_ACCOUNT_ENABLED=true` 仅用于独立运行原单体；通过 Gateway 运行 P2 微服务链路时必须保持为 `false`，避免原单体与 `account-service` 同时写账户数据。Windows 环境请使用 `mvnw.cmd`，并在启动前通过 PowerShell 或系统设置配置相同的环境变量。

常用配置及对应环境变量：

| 配置 | 环境变量 | 本地示例 |
| --- | --- | --- |
| MySQL 地址 | `SPRING_DATASOURCE_URL` | `jdbc:mysql://127.0.0.1:3306/spring` |
| MySQL 用户名 | `SPRING_DATASOURCE_USERNAME` | `root` |
| MySQL 密码 | `SPRING_DATASOURCE_PASSWORD` | `hard-local-password` |
| Redis 地址 | `SPRING_DATA_REDIS_HOST` | `127.0.0.1` |
| Redis 端口 | `SPRING_DATA_REDIS_PORT` | `6379` |
| JWT 密钥 | `APP_JWT_SECRET` | 至少 32 字节的随机字符串 |
| 原单体账户接口 | `APP_LEGACY_CONTROLLERS_ACCOUNT_ENABLED` | 独立运行单体时为 `true`，P2 微服务模式为 `false` |
| 应用端口 | `SERVER_PORT` | `8080` |

本项目仅用于本地学习，表中的密码和 JWT 密钥都是为了简化联调而保留的本地示例值，不应复制到任何真实环境。

## 5. 验证启动结果

查看应用整体健康状态：

```bash
curl http://127.0.0.1:8080/actuator/health
```

分别检查应用、MySQL 和 Redis：

```bash
curl http://127.0.0.1:8080/actuator/health/application
curl http://127.0.0.1:8080/actuator/health/mysql
curl http://127.0.0.1:8080/actuator/health/redis
```

返回结果中的 `status` 为 `UP` 表示对应组件可用。如果 MySQL 或 Redis 未启动，整体健康状态会显示 `DOWN`，并可能返回 HTTP 503。

接口文档地址：

```text
http://127.0.0.1:8080/swagger-ui.html
```

## 日常启动与停止

已经创建过容器时，后续直接启动即可：

```bash
docker start hard-mysql hard-redis
```

停止本地依赖：

```bash
docker stop hard-mysql hard-redis
```

容器停止不会删除数据库数据，下次启动仍会保留。

## 运行测试

```bash
./mvnw test
```

## 常见问题

### 容器名称已存在

如果再次执行 `docker run` 时提示容器名称已存在，说明容器已经创建，使用以下命令启动即可：

```bash
docker start hard-mysql hard-redis
```

### MySQL 登录失败

确认应用的 `SPRING_DATASOURCE_PASSWORD` 与创建 MySQL 容器时设置的 `MYSQL_ROOT_PASSWORD` 一致。已经创建的容器不会因重新设置环境变量而自动修改密码。

### 健康检查为 DOWN

先确认两个依赖容器都处于运行状态：

```bash
docker ps --filter name=hard-mysql --filter name=hard-redis
```

再分别查看容器日志：

```bash
docker logs hard-mysql
docker logs hard-redis
```

### Maven Wrapper 无法执行

在 macOS 或 Linux 上为脚本增加执行权限后重试：

```bash
chmod +x mvnw
```
