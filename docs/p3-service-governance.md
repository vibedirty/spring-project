# P3 OpenFeign、LoadBalancer 与 Sentinel

P3 消除了订单域对 `user`、`user_address` 的直接读取。`spring-java-service` 通过 `AccountServiceClient` 按 Nacos 服务名调用 `account-service`，由 Spring Cloud LoadBalancer 在多个账户实例间选择目标。

## 已实现边界

- `account-service` 提供 `UserSummary` 与 `AddressSnapshot` 两个稳定内部 DTO，不返回持久化 Entity。
- Feign 透传 `Authorization` 与 `X-Request-ID`，连接超时 2 秒、读取超时 3 秒、日志级别为 BASIC。
- 创建订单在 `TransactionTemplate.execute` 之前查询地址和用户快照，远程等待不占用本地数据库事务。
- 支付、取消、收货和发货日志通过账户契约取得操作者昵称；订单列表与详情继续只读订单商品、地址和操作日志快照。
- Gateway 不路由 `/internal/**`，内部接口仍要求 JWT。
- Gateway 的自动重试仅允许 GET；注册、登录、地址写入等 POST 不进行无条件重试。

## Sentinel 资源与规则

| 应用 | 资源 | 规则 |
| --- | --- | --- |
| Gateway | `account-auth-api`、`account-address-api`、`spring-java-api` | 路由 QPS |
| spring-java-service | `account-query-user-summary` | 并发线程数、慢调用比例 |
| spring-java-service | `account-query-address-snapshot` | QPS、异常比例 |
| account-service | `account-internal-user-summary` | 并发线程数、慢调用比例 |
| account-service | `account-internal-address-snapshot` | QPS、异常比例 |

规则源文件位于 `infra/nacos/config/*-sentinel-*.json`，`infra/nacos/bootstrap.sh` 将其发布到两个 Namespace 的 `SENTINEL_GROUP`。应用重启后会重新从 Nacos 加载规则。

## 启动

从仓库根目录执行：

```bash
docker compose -f infra/nacos/compose.yaml up -d --build
sh infra/nacos/bootstrap.sh

export JAVA_HOME=/opt/homebrew/opt/openjdk@17
cd spring-cloud-backend
mvn clean package
cd ..
```

然后按 `spring-cloud-backend/spring-cloud-services/account-service/README.md` 启动 Gateway、原单体和两个账户实例。Sentinel Dashboard 位于 `http://127.0.0.1:8858`，首次有流量后会显示三个应用。

## 故障实验

先登录取得 Token，并准备一个属于当前用户的地址 ID。创建订单会同时触发用户摘要与地址快照调用。

1. 全部停止账户实例，再创建、支付、取消或收货。预期响应体业务码为 `503`，错误类型为 `UNAVAILABLE`；订单列表和历史详情仍可查询。
2. 在 Nacos `SERVICE_GROUP/account-service.yaml` 将 `app.p3.simulation.delay-ms` 改为 `4000`。读取超时为 3 秒，预期业务码 `504`，错误类型为 `TIMEOUT`。连续请求达到最小采样数后，慢调用熔断打开；10 秒窗口后进入探测，并在延迟恢复为 0 后关闭。
3. 将 `force-error` 改为 `true`，连续请求地址快照。达到异常比例阈值后，预期错误类型由依赖异常变为 `CIRCUIT_OPEN`；恢复为 `false` 后观察半开探测和关闭。
4. 使用并发请求超过 flow JSON 中的阈值。Gateway 返回 HTTP 429；Feign/账户内部资源返回业务码 429，错误类型为 `RATE_LIMITED`。
5. 重启任一 Java 服务，确认 Dashboard 中资源重新出现，并确认规则仍从 Nacos 生效。

故障类型由 `AccountFailureType` 显式区分：`TIMEOUT`、`RATE_LIMITED`、`CIRCUIT_OPEN`、`UNAVAILABLE`。账户服务返回的 400、404、409 保留为业务错误，不会被伪装成降级成功。

## 静态边界检查

```bash
rg -n 'UserMapper|AddressService|UserAddress|com\.cat\.hard\.user' \
  spring-cloud-backend/spring-java/src/main/java/com/cat/hard/order
```

命令应无输出。历史订单展示不调用 `AccountQueryService`，账户服务全部停止时仍可使用已有订单快照。
