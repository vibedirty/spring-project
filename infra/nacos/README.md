# P0-P3 Nacos 与 Sentinel 本地环境

P0 只将 Nacos 运行在 Docker 中。Gateway、原单体和后续业务服务均直接运行在 macOS 宿主机 JVM，不构建 Docker 镜像。

## 固定基线

| 项目 | 地址或版本 |
| --- | --- |
| Nacos Server | `nacos/nacos-server:v3.1.1` |
| Sentinel Dashboard | `http://127.0.0.1:8858`（1.8.9） |
| Client/API | `127.0.0.1:8848` |
| Console | `http://127.0.0.1:8849` |
| gRPC | `127.0.0.1:9848` |
| 存储 | Nacos 内置 Derby + Docker named volume |

所有端口只绑定 `127.0.0.1`，该实例仅供本机学习使用。客户端访问鉴权默认关闭，不得将这些端口暴露到公网或共享网络。

## 启动和检查

在仓库根目录执行：

```bash
docker compose -f infra/nacos/compose.yaml up -d
docker compose -f infra/nacos/compose.yaml ps
docker compose -f infra/nacos/compose.yaml logs -f nacos
docker compose -f infra/nacos/compose.yaml logs -f sentinel-dashboard
```

日志出现 Nacos 启动成功信息后，打开 `http://127.0.0.1:8849`。Nacos 3 首次进入控制台时，按页面提示初始化管理员密码。

服务端就绪检查使用 Nacos 3 只读 Client API。旧的
`/nacos/v1/console/health/readiness` 在 Nacos 3 中已返回 HTTP 410，不能继续作为检查端点：

```bash
curl --fail \
  'http://127.0.0.1:8848/nacos/v3/client/ns/instance/list?serviceName=p0-health-check'
```

## 初始化命名空间和配置

首次启动后准备本地管理员密码并执行初始化：

```bash
cp infra/nacos/.env.example infra/nacos/.env
# 编辑 infra/nacos/.env 中的 NACOS_ADMIN_PASSWORD
sh infra/nacos/bootstrap.sh
```

脚本会初始化或登录本地 `nacos` 管理员，创建 `hard-dev`、`hard-test`，并向两个 Namespace 发布：

- `COMMON_GROUP/common.yaml`：P0 基线标记；
- `SERVICE_GROUP/gateway-service.yaml`：P1 Gateway 路由、超时和 CORS；
- `SERVICE_GROUP/spring-java-service.yaml`：P1 原单体服务配置。
- `SERVICE_GROUP/account-service.yaml`：P2/P3 账户服务配置和故障模拟开关；
- `SENTINEL_GROUP/*-sentinel-*.json`：P3 Gateway、Feign 和账户内部接口治理规则。

配置源文件保存在 `infra/nacos/config/`，应先修改版本库中的源文件，再重新执行脚本发布，避免 Nacos 控制台内容与代码库长期漂移。真实 `.env` 已被仓库根 `.gitignore` 忽略。

## 停止、重启和重置

停止容器但保留配置数据：

```bash
docker compose -f infra/nacos/compose.yaml down
```

重启并验证配置仍然存在：

```bash
docker compose -f infra/nacos/compose.yaml up -d
```

完全重置会删除 Nacos 配置、命名空间和用户数据，只能在确认不需要保留本地数据时执行：

```bash
docker compose -f infra/nacos/compose.yaml down -v
```

## Nacos 命名模型

环境使用 Namespace 隔离，配置类型使用 Group 隔离。配置中的 Namespace 必须填写 Namespace ID，而不是只填写控制台显示名称。

| 类型 | 约定值 |
| --- | --- |
| 开发 Namespace ID | `hard-dev` |
| 测试 Namespace ID | `hard-test` |
| 服务发现 Group | `HARD_GROUP` |
| 公共配置 Group | `COMMON_GROUP` |
| 服务配置 Group | `SERVICE_GROUP` |
| Sentinel 规则 Group | `SENTINEL_GROUP` |
| 本地 Cluster | `LOCAL` |

Data ID 不重复携带环境名，环境由 Namespace 唯一表达：

```text
COMMON_GROUP
  common.yaml

SERVICE_GROUP
  spring-java-service.yaml
  gateway-service.yaml
  account-service.yaml
  cart-service.yaml
  product-service.yaml
  order-service.yaml

SENTINEL_GROUP
  gateway-service-sentinel-gateway-flow.json
  spring-java-service-sentinel-flow.json
  spring-java-service-sentinel-degrade.json
  account-service-sentinel-flow.json
  account-service-sentinel-degrade.json
```

P0 定义并验证该模型；P1 已实现应用从 Nacos 导入配置、服务注册、动态 Gateway 路由和多实例负载均衡。

## P1 服务注册检查

启动应用后，可以用 Nacos 3 Client API 检查 `LOCAL` Cluster。查询实例列表时必须带 `clusterName=LOCAL`：

```bash
curl --fail \
  'http://127.0.0.1:8848/nacos/v3/client/ns/instance/list?serviceName=gateway-service&groupName=HARD_GROUP&namespaceId=hard-dev&clusterName=LOCAL'

curl --fail \
  'http://127.0.0.1:8848/nacos/v3/client/ns/instance/list?serviceName=spring-java-service&groupName=HARD_GROUP&namespaceId=hard-dev&clusterName=LOCAL'
```

## 宿主机应用约定

- Java 应用使用 `127.0.0.1:8848` 连接 Nacos。
- 同一服务的多个实例使用相同服务名和不同端口。
- 本机开发时实例可以注册 `127.0.0.1`；如果 VPN 或虚拟网卡导致客户端选择错误地址，通过 `SERVICE_IP` 显式覆盖。
- `server.port`、`spring.application.name`、Nacos 连接参数和凭据保留在应用本地配置中。
- 数据库密码、JWT Secret 等敏感值不得直接写入 Nacos，应继续通过环境变量注入。

## 常见问题

### Console 无法打开

先检查容器状态和日志：

```bash
docker compose -f infra/nacos/compose.yaml ps
docker compose -f infra/nacos/compose.yaml logs nacos
```

### 端口已被占用

检查 `8848`、`8849` 和 `9848`。Nacos Console 使用宿主机 `8849`，不会占用原单体的 `8080`。

### Nacos 一直处于启动中

检查 Docker Desktop 可用内存。当前 Compose 为 Nacos JVM 配置了 256 MiB 初始堆和 512 MiB 最大堆，可通过 `.env` 覆盖。

### 应用注册了错误 IP

macOS 上的 VPN、Docker 和虚拟网卡可能影响自动地址选择。在 P1 的应用配置中使用环境变量显式指定：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        ip: ${SERVICE_IP:127.0.0.1}
```
