# P2/P3 Account Service

`account-service` 是从单体中抽离出的第一个业务微服务，负责用户认证（`auth`）、用户信息（`user`）与收货地址（`address`）。对外保持原有的公开 API 契约不变，前端通过 `gateway-service` 访问。

## 1. 边界与设计约定

- **数据库所有权**：在共享 `spring` 数据库下，`account-service` 是 `user`、`user_address` 表的唯一合法写入者。
- **Redis 所有权**：拥有 `auth:jwt:session:*`（JWT 会话）、`auth:login:failures:*`（登录限流计数）Key 的唯一写入权。
- **跨服务 JWT**：与原单体共享对称秘钥及 Claims 定义（`userId`、`role`、`jti`、`iat`、`exp`），签发的 Token 可直接无缝用于后续服务（购物车、订单等）鉴权。
- **原单体平滑过渡**：原单体通过 `@ConditionalOnProperty(prefix = "app.legacy-controllers", name = "account-enabled")` 配置已在微服务模式下关闭对外账户写 Controller，杜绝新旧双写。
- **本地配置范围**：项目仅用于本地学习，`application-dev.yml` 中的数据库密码和固定 JWT Secret 是本地示例值，用于简化多服务联调，不得用于真实环境或发布到 Nacos。
- **P3 内部契约**：提供 `GET /internal/users/{userId}/summary` 与 `GET /internal/users/{userId}/addresses/{addressId}`；返回 `UserSummary`、`AddressSnapshot`，不暴露 Entity。内部路径要求 JWT，且不经 Gateway 对外路由。
- **故障实验**：Nacos 的 `app.p3.simulation.delay-ms` 和 `force-error` 可分别制造慢调用和异常，Sentinel 规则来自 `SENTINEL_GROUP`。

## 2. 前置条件

- JDK 17；
- Docker 中的 Nacos（`127.0.0.1:8848`）已启动；
- 本机 MySQL（3306）、Redis（6379）已启动；
- 已执行 `sh infra/nacos/bootstrap.sh` 发布服务配置和 P3 Sentinel JSON 规则。

构建所有后端模块：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
cd spring-cloud-backend
mvn clean package -DskipTests
cd ..
```

## 3. 启动与多实例演练

### 3.1 启动服务链路

分别在不同终端启动 Gateway、原单体与两个 Account Service 实例：

1. **Gateway (`9000`)**：
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
SERVER_PORT=9000 SERVICE_IP=127.0.0.1 \
  "$JAVA_HOME/bin/java" -jar spring-cloud-backend/spring-cloud-services/gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar
```

2. **Account Service 实例 1 (`8101`)**：
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
SERVER_PORT=8101 SERVICE_IP=127.0.0.1 \
  "$JAVA_HOME/bin/java" -jar spring-cloud-backend/spring-cloud-services/account-service/target/account-service-0.0.1-SNAPSHOT.jar
```

3. **Account Service 实例 2 (`8102`)**：
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
SERVER_PORT=8102 SERVICE_IP=127.0.0.1 \
  "$JAVA_HOME/bin/java" -jar spring-cloud-backend/spring-cloud-services/account-service/target/account-service-0.0.1-SNAPSHOT.jar
```

4. **原单体 `spring-java-service` (`8080`)**：
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
SERVER_PORT=8080 SERVICE_IP=127.0.0.1 \
  "$JAVA_HOME/bin/java" -jar spring-cloud-backend/spring-java/target/hard-0.0.1-SNAPSHOT.jar
```

### 3.2 环境变量说明

| 变量名 | 默认值 | 用途 |
| --- | --- | --- |
| `SERVER_PORT` | `8101` | 服务端口 |
| `SERVICE_IP` | `127.0.0.1` | 注册到 Nacos 的实例 IP |
| `NACOS_SERVER_ADDR` | `127.0.0.1:8848` | Nacos Server 地址 |
| `NACOS_NAMESPACE` | `hard-dev` | Nacos 命名空间 |
| `SPRING_PROFILES_ACTIVE` | `dev` | 激活 Profile |
| `ACCOUNT_UPSTREAM_URI` | `lb://account-service` | Gateway 路由目标，可用于指定固定单实例回退 |

## 4. 接口验证与故障转移

### 4.1 认证与地址接口验证

1. **用户注册**：
```bash
curl -X POST http://127.0.0.1:9000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_user","password":"password123","nickname":"测试用户"}'
```

2. **用户登录**：
```bash
curl -X POST http://127.0.0.1:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_user","password":"password123"}'
```

3. **使用获取到的 Token 访问地址管理**：
```bash
curl -X POST http://127.0.0.1:9000/api/addresses \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"receiverName":"张三","phone":"13800000000","province":"广东省","city":"深圳市","district":"南山区","detailAddress":"科技园1号","isDefault":1}'
```

4. **查询地址列表**：
```bash
curl -X GET http://127.0.0.1:9000/api/addresses \
  -H "Authorization: Bearer <TOKEN>"
```

### 4.2 负载均衡与单实例下线验证

- 连续调用 `/api/auth/login` 或 `/api/addresses`，观察请求在 `8101` 与 `8102` 间负载均衡分发。
- 手动 `kill` 停止 `8101` 实例，继续发起请求；若 Nacos 尚未摘除该实例，Gateway 会对连接建立失败重试一次并重新选择实例。重试仅覆盖 `ConnectException`，不会对业务错误或响应超时进行宽泛重试。
- 重启 `8101` 实例，Nacos 心跳恢复后重新加入负载均衡池。

### 4.3 路由回退方案

若需临时将账户路由切换为固定单实例调试：
启动 Gateway 时通过环境变量覆盖上游地址：
```bash
ACCOUNT_UPSTREAM_URI=http://127.0.0.1:8101 SERVER_PORT=9000 ...
```
或直接在 Nacos 控制台修改 `gateway-service.yaml` 并热推送生效。
