# P1 Gateway + Nacos

`gateway-service` 是两个前端的统一 API 入口。默认将 `/api/**` 通过 Nacos 服务发现和 Spring Cloud LoadBalancer 转发到 `spring-java-service`，自身不连接 MySQL，也不承载业务 Service。

## 前置条件

- JDK 17；
- Docker 中的 Nacos 已启动；
- 本机 MySQL、Redis 可供原单体使用；
- 已执行 `sh infra/nacos/bootstrap.sh` 发布 P1 配置。

以下命令均从仓库根目录执行。先构建全部后端模块：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd spring-cloud-backend
./spring-java/mvnw clean package -DskipTests
cd ..
```

## 启动 P1 链路

分别在三个终端中启动两个原单体实例和一个 Gateway：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
SERVER_PORT=8081 SERVICE_IP=127.0.0.1 \
  "$JAVA_HOME/bin/java" -jar spring-cloud-backend/spring-java/target/hard-0.0.1-SNAPSHOT.jar
```

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
SERVER_PORT=8082 SERVICE_IP=127.0.0.1 \
  "$JAVA_HOME/bin/java" -jar spring-cloud-backend/spring-java/target/hard-0.0.1-SNAPSHOT.jar
```

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
SERVER_PORT=9000 SERVICE_IP=127.0.0.1 \
  "$JAVA_HOME/bin/java" -jar spring-cloud-backend/spring-cloud-services/gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar
```

如果 `8080` 没有被其他进程使用，也可以让其中一个原单体使用默认端口。微服务应用均直接运行在宿主机 JVM，只有 Nacos 运行在 Docker 中。

主要环境变量：

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `SERVER_PORT` | Gateway `9000`，原单体 `8080` | 同一服务启动不同端口实例 |
| `SERVICE_IP` | `127.0.0.1` | 注册到 Nacos 的宿主机地址 |
| `NACOS_SERVER_ADDR` | `127.0.0.1:8848` | Nacos Client/API 地址 |
| `NACOS_NAMESPACE` | `hard-dev` | 配置与注册发现 Namespace ID |
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring Profile |
| `GATEWAY_UPSTREAM_URI` | `lb://spring-java-service` | Gateway 上游路由，可切换固定地址 |

## 验收

健康检查和业务请求：

```bash
curl --fail http://127.0.0.1:9000/actuator/health
curl -i http://127.0.0.1:9000/api/categories
```

响应应包含：

- `X-Request-ID`：Gateway 保留调用方传入值，缺失时自动生成；
- `X-Service-Instance`：显示实际处理请求的实例，例如 `spring-java-service:8081`。

连续请求可观察两个实例被选中：

```bash
for request_number in 1 2 3 4 5 6; do
  curl -sS -D - -o /dev/null \
    -H "X-Request-ID: p1-$request_number" \
    http://127.0.0.1:9000/api/categories
done
```

停止其中一个原单体后再次连续请求，Gateway 应只返回存活实例的 `X-Service-Instance`，且请求保持成功。重新启动该实例后，Nacos 会把它重新加入可用实例列表。

检查跨域预检：

```bash
curl -i -X OPTIONS http://127.0.0.1:9000/api/categories \
  -H 'Origin: http://localhost:5173' \
  -H 'Access-Control-Request-Method: GET'
```

## 动态配置与固定地址回退

Gateway 路由源文件是 `infra/nacos/config/gateway-service.yaml`。修改后执行：

```bash
sh infra/nacos/bootstrap.sh
```

Nacos 会把配置变更推送给运行中的 Gateway。可通过修改 `info.p1.route-mode`，重新发布后访问 `/actuator/info`，确认配置已刷新。

需要绕过服务发现、临时回退到固定单体地址时，在启动 Gateway 前设置：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
GATEWAY_UPSTREAM_URI=http://127.0.0.1:8080 \
SERVER_PORT=9000 SERVICE_IP=127.0.0.1 \
  "$JAVA_HOME/bin/java" -jar spring-cloud-backend/spring-cloud-services/gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar
```

该变量替换 Nacos 路由中的 URI 占位符，不需要改业务代码。恢复默认值并重启 Gateway 后重新使用 `lb://spring-java-service`。

## 前端

用户端和管理端浏览器均直接跨域访问 `http://localhost:9000`，Vite 不代理 API 请求：

```bash
cd frontend && npm run dev
cd frontend-admin && npm run dev
```

如需临时覆盖，在对应前端环境文件中设置 `VITE_API_BASE_URL`。管理端通过 `/management/spring-java/actuator/health/**` 检查单体健康状态，该路由只暴露健康检查并通过 Nacos 选择可用实例；Gateway 自身健康状态仍位于 `/actuator/health`。

## P1 完成状态

P1 已于 2026-08-27 完成。验收时 Gateway 健康状态为 `UP`；Nacos 中有 Gateway `9000` 和原单体 `8081/8082` 三个健康实例；`/api/categories` 经过 Gateway 返回 `200`；双实例分发、单实例下线恢复、CORS、Nacos 配置热刷新和固定地址回退均正常。用户端和管理端浏览器均直接访问 Gateway，不依赖 Vite API 代理。

完整 Maven 测试共运行原单体 357 项，其中 356 项通过，1 项既有的 `OrderIdempotencyServiceTests.shouldAcquireUserScopedTokenWithExpiration` 因测试桩 TTL 参数与当前实现不一致而报错。Gateway 新增的 2 项 Request ID 测试全部通过，完整打包成功。
