# Spring Java 微服务拆分与学习计划

## 1. 文档目的

本文档对应 `plan.md` 中的开发规划 191 和 192，用于指导当前 `spring-java` 模块化单体逐步演进为 Spring Cloud Alibaba 微服务系统。

本项目是学习级项目，首要目标不是以最少组件承载当前业务量，而是通过可运行、可验证的阶段任务系统学习：

- API Gateway；
- 服务注册、发现和配置管理；
- 服务间调用和客户端负载均衡；
- 限流、熔断、降级和隔离；
- JWT 跨服务认证；
- 消息队列、Outbox 和幂等消费；
- Saga、补偿任务与分布式事务；
- 指标、日志、链路追踪和故障演练。

所有微服务改造只在长期 `microservices` 分支演进，不合并回保留单体版本的 `main` 分支。

## 2. 已确定的方案

### 2.1 第一轮服务边界

第一轮形成 4 个业务服务和 1 个 Gateway：

1. `gateway-service`
2. `account-service`
3. `cart-service`
4. `product-service`
5. `order-service`

其中：

- `account-service` 包含 `auth`、`user` 和 `address`。
- `product-service` 包含 `category`、`product` 和 `stock`。
- 模拟支付、发货和确认收货暂时属于 `order-service`。
- 第一轮不单独拆 `inventory-service`、`payment-service` 和 `fulfillment-service`。

### 2.2 数据库方案

第一轮所有服务共享现有 MySQL 实例和 `spring` 数据库，不做物理拆库，也不迁移历史数据。

共享数据库不等于无边界。各服务必须遵守逻辑所有权和单写原则：

| 服务 | 逻辑拥有的 MySQL 表 | 允许写入者 |
| --- | --- | --- |
| `account-service` | `user`、`user_address` | 仅 `account-service` |
| `product-service` | `category`、`product`、`stock_log` | 仅 `product-service` |
| `order-service` | `orders`、`order_item`、`order_address`、`order_operate_log`、后续 `outbox_event` | 仅 `order-service` |
| `cart-service` | 无 MySQL 表 | 不适用 |

拆分初期允许原单体保留少量临时读取，随后通过 OpenFeign 内部接口替换。新服务接管某张表的写接口后，原单体不得继续写该表。

第一轮保留现有数据库外键。后续学习“每服务独立 Schema/数据库”时，再删除跨服务外键并执行数据迁移实验。

### 2.3 Redis 数据归属

所有服务可以共享同一个 Redis 实例，但必须按 Key 前缀划分逻辑所有权：

| 服务 | Redis Key |
| --- | --- |
| `account-service` | `auth:jwt:session:*`、`auth:login:failures:*` |
| `cart-service` | `cart:{userId}` |
| `product-service` | `cache:product:*`、商品缓存锁 |
| `order-service` | `order:idempotency:*`、`lock:order:status:*`、订单超时 ZSet |
| `gateway-service` | 可选的网关限流数据 |

### 2.4 JWT 方案

第一轮保持当前对称密钥 JWT：

- `account-service` 负责签发 Token；
- Gateway 和各业务服务使用同一 Secret 验签；
- Gateway 必须原样转发 `Authorization` Header；
- 业务服务仍需进行必要的本地鉴权，不能只依赖前端提交的用户 ID。

完成基础拆分后，再增加非对称签名、密钥轮换和 Token 撤销事件实验。

## 3. 技术栈和学习阶段

| 能力 | 技术 | 首次引入阶段 |
| --- | --- | --- |
| API 网关 | Spring Cloud Gateway | P1 |
| 注册中心 | Nacos Discovery | P0～P1 |
| 配置中心 | Nacos Config | P1 |
| 服务调用 | OpenFeign | P3 |
| 客户端负载均衡 | Spring Cloud LoadBalancer | P1、P3 |
| 限流、熔断和降级 | Sentinel | P3 |
| 消息队列 | RocketMQ | P6 |
| 可靠事件 | Outbox + 幂等消费 | P6 |
| 跨服务流程 | Saga + 补偿任务 | P6 |
| 分布式事务对比 | Seata AT/TCC 实验 | P7 |
| 健康检查和指标 | Actuator + Micrometer | P1 起持续完善 |
| 链路追踪 | Micrometer Tracing | P7 |
| 缓存和锁 | Redis + Redisson | 沿用并按服务归属迁移 |

## 4. 版本基线

### 4.1 推荐稳定组合

为了使用已经正式发布的 Spring Cloud Alibaba 版本，`microservices` 分支推荐统一到以下学习基线：

| 组件 | 版本 |
| --- | --- |
| Java | 17 |
| Spring Boot | 4.0.0 |
| Spring Cloud | 2025.1.0 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Nacos Client | 由 Spring Cloud Alibaba BOM 管理 |

当前单体使用 Spring Boot 4.1.0。P0 需要先在 `microservices` 分支验证降级到上述稳定组合是否能通过构建和回归测试，再让 Gateway、单体和后续服务采用统一 BOM。

如果不接受降级，则只能等待兼容 Spring Boot 4.1.0 + Spring Cloud 2025.1.2 的 Spring Cloud Alibaba 稳定版本，或在学习环境使用 Snapshot。默认路线优先选择稳定发行版，不使用 Snapshot 作为长期基线。

### 4.2 版本管理规则

- 使用 Maven BOM 管理 Spring Cloud 和 Spring Cloud Alibaba 版本。
- 业务模块不得单独覆盖 Nacos、Sentinel、RocketMQ、Seata 等传递依赖版本。
- 所有服务统一 Java 17。
- 每次升级先创建独立分支并执行全量回归测试。
- 版本组合、升级原因和验证结果写入依赖说明文档。

## 5. 目标架构

### 5.1 开发环境部署基线

当前学习阶段只将 Nacos 运行在 Docker 中，不将 Gateway、原单体和业务微服务容器化。

```text
macOS 宿主机
  |-- Java 应用：Gateway、原单体和各业务服务
  |     `-- 通过 IDE 或 Maven 直接启动，使用不同端口
  |
  `-- Docker
        `-- Nacos Server（单机模式）
```

约定：

- Java 应用统一在宿主机 JVM 运行，便于 IDE 调试、热更新和多实例实验。
- Java 应用通过 `127.0.0.1:8848` 访问 Nacos。
- 同一宿主机上的服务实例可以向 Nacos 注册 `127.0.0.1` 和各自端口。如果 macOS 受 VPN 或虚拟网卡影响选错 IP，则通过环境变量显式指定注册 IP。
- Nacos 客户端端口使用 `8848`，gRPC 端口使用 `9848`。Nacos 3 控制台的容器端口 `8080` 映射到宿主机 `8849`，避免与原单体的 `8080` 冲突。
- P0 的 Docker Compose 只管理 Nacos；MySQL 和 Redis 继续沿用当前项目的本地运行方式。
- 微服务容器化不作为当前各阶段的验收条件；如后续需要学习容器编排，再建立独立实验。

第一阶段：Gateway 通过 Nacos 发现原单体。

```text
                         Nacos
                 注册中心 + 配置中心
                      ^         ^
                      |         |
前端 ---> gateway-service ---> spring-java-service
              |                  实例 1
              `---------------> spring-java-service
                                 实例 2
```

账户服务拆出后：

```text
                             Nacos
                    注册、发现、配置管理
                   /         |          \
                  v          v           v
前端 ---> gateway-service  account-service  spring-java-service
                              ^                  |
                              `---- Feign -------'
```

全部服务拆出后：

```text
前端
  |
  v
gateway-service
  |-- account-service
  |-- cart-service -----> product-service
  |-- product-service
  `-- order-service ----> account-service
                    |---> cart-service
                    `---> product-service

所有应用注册到 Nacos；配置由 Nacos Config 分组管理。
订单事件通过 RocketMQ 传递。
```

## 6. 综合优先级和难度

难度采用 1～5 级，数值越大表示越困难。排序同时考虑学习依赖、业务耦合和阶段可验证性，不按当前业务体量删减学习内容。

| 顺序 | 阶段 | 任务 | 状态 | 优先级 | 难度 | 主要学习内容 |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | P0 | 环境、版本和 Nacos 基线 | 已完成 | 最高 | 2 | BOM、版本兼容、Nacos 部署、服务命名和配置模型 |
| 1 | P1 | Gateway + Nacos | 已完成 | 最高 | 2 | Gateway、注册发现、`lb://`、动态路由、多实例负载均衡 |
| 2 | P2 | Account Service | 待开始 | 高 | 3 | 业务服务抽取、共享数据库边界、JWT 跨服务使用、路由切换 |
| 3 | P3 | 服务通信与治理 | 待开始 | 高 | 3 | OpenFeign、LoadBalancer、Sentinel、超时、熔断和降级 |
| 4 | P4 | Cart Service | 待开始 | 中高 | 3 | Redis 数据所有权、商品批量查询、远程依赖降级 |
| 5 | P5 | Product Service | 待开始 | 高 | 4 | 分类、商品、库存、缓存、幂等库存接口 |
| 6 | P6 | Order Service | 待开始 | 最高 | 5 | RocketMQ、Outbox、Saga、幂等消费、补偿任务 |
| 7 | P7 | 高级实验和可观测性 | 待开始 | 中高 | 4 | Seata、链路追踪、故障演练、独立 Schema 对比实验 |

推荐顺序：

```text
P0 环境和版本基线
  -> P1 Gateway + Nacos
  -> P2 Account Service
  -> P3 OpenFeign + Sentinel
  -> P4 Cart Service
  -> P5 Product Service
  -> P6 Order Service + RocketMQ + Saga
  -> P7 Seata、Tracing 和故障演练
```

## 7. 分阶段实施计划

### P0：环境、版本和 Nacos 基线

#### 目标

建立统一、可重复的微服务开发环境，验证稳定版本组合，并在不改变业务路由的前提下启动 Nacos。

#### 主要任务

1. 在 `microservices` 分支创建 `feature/ms-p0-environment`。
2. 记录当前 Java 17、MySQL、Redis 和单体测试基线；已知的订单幂等 TTL 测试约定可以作为已接受差异记录。
3. 创建独立的 Maven 父工程和服务聚合目录。仓库根目录不是 Maven 工程；P0 只创建聚合 POM，各服务目录在对应阶段开始时再创建：

```text
spring-cloud-backend/
  pom.xml                         # 统一版本和 Reactor 入口
  spring-java/                    # 迁移期间保留的模块化单体
  spring-cloud-services/
    pom.xml                       # 后续微服务模块聚合入口
```

4. 在父 POM 中导入 Spring Cloud 和 Spring Cloud Alibaba BOM。
5. 在 `microservices` 分支验证 `spring-java` 使用稳定版本组合后的编译、测试和启动。
6. 使用只包含 Nacos 的 Docker Compose 启动 Nacos 单机模式，并通过 Docker Volume 持久化必要数据。Gateway、原单体和后续业务服务均不创建 Docker 镜像。
7. 学习并定义 Nacos：
   - Namespace：区分 `dev`、`test`；
   - Group：区分基础配置和业务服务配置；
   - Data ID：按服务命名，环境由 Namespace 唯一表达；
   - 服务名、实例 IP、端口和元数据。
8. 规划端口，建议：
   - Nacos Client/API：`8848`
   - Nacos Console：`8849`（映射容器端口 `8080`）
   - Nacos gRPC：`9848`
   - Gateway：`9000`
   - 原单体实例：`8080`、`8081`
   - Account：`8101`
   - Cart：`8102`
   - Product：`8103`
   - Order：`8104`
9. 为每个应用保留本地最小配置，其余配置逐步迁入 Nacos Config。

#### 验收标准

- Nacos Console 可以访问。
- Nacos 重启后配置仍可恢复。
- Nacos 是 P0 中唯一由 Docker Compose 管理的组件，控制台与原单体没有端口冲突。
- Gateway、原单体和业务服务可以在宿主机 JVM 中启动并访问 `127.0.0.1:8848`。
- Maven BOM 生效且没有手工覆盖核心组件版本。
- 单体在选定版本基线上能够构建、启动并完成主要回归测试。
- 形成环境启动、停止、重置和排错说明。

### P1：Gateway + Nacos

#### 目标

让前端通过 Gateway 访问原单体，并完整学习服务注册、发现、动态路由和客户端负载均衡。

#### 主要任务

1. 创建 `feature/ms-p1-gateway`。
2. 创建 `gateway-service`，接入：
   - Spring Cloud Gateway；
   - Nacos Discovery；
   - Nacos Config；
   - Actuator。
3. 让 `spring-java` 以 `spring-java-service` 注册到 Nacos。
4. 让 Gateway 以 `gateway-service` 注册到 Nacos。
5. 初始路由全部转发给原单体：

```text
/api/** -> lb://spring-java-service
```

6. 将 Gateway 路由、超时和 CORS 配置放入 Nacos Config。
7. 保证 Gateway 原样转发：
   - `Authorization`；
   - Request ID / Trace ID；
   - 请求体和查询参数；
   - 业务错误响应。
8. 在宿主机 JVM 中启动两个不同端口的 `spring-java-service` 实例，验证 LoadBalancer 分发请求。
9. 进行服务上下线实验：
   - 停止一个实例；
   - 观察 Nacos 健康状态；
   - 验证 Gateway 不再向失效实例转发。
10. 修改 Nacos 中的路由或超时配置，验证配置加载行为。
11. 将用户端和管理端 API 地址统一改为 Gateway 地址。

#### 验收标准

- 两个前端只访问 Gateway 即可完成现有业务流程。
- Gateway 不连接业务 MySQL，也不包含业务 Service。
- Nacos 中可以看到 Gateway 和单体实例。
- `lb://spring-java-service` 路由正常。
- 单体多实例能够被轮询或按实际负载均衡策略访问。
- 一个实例下线不会导致全部请求失败。
- Gateway 路由可以回退为原单体固定地址。

#### 完成记录（2026-08-27）

P1 已在 `feature/ms-p1-gateway` 实现并完成技术链路验收，可合并回 `microservices`：

- 新增 `gateway-service`，通过 Nacos Config 加载路由、超时和 CORS；
- Gateway 和原单体分别以 `gateway-service`、`spring-java-service` 注册到 Nacos；
- `/api/**` 默认通过 `lb://spring-java-service` 转发；
- 已用宿主机端口 `8081`、`8082` 验证双实例分发和单实例下线恢复；
- 已验证 Nacos 配置推送能够在 Gateway 运行期间触发刷新；
- 用户端和管理端浏览器直接跨域访问 `http://localhost:9000`，不使用 Vite 本地代理；
- 通过 `GATEWAY_UPSTREAM_URI=http://127.0.0.1:8080` 可将路由回退为固定单体地址。

启动、回退和复验命令见 `spring-cloud-backend/spring-cloud-services/gateway-service/README.md`。

### P2：Account Service，共享数据库

#### 目标

抽取第一个业务服务，学习服务注册、共享数据库下的数据所有权、JWT 跨服务认证和 Gateway 路由切换。

#### 主要任务

1. 创建 `feature/ms-p2-account`。
2. 创建 `account-service`，迁移或重构以下模块：
   - `auth`
   - `user`
   - `address`
3. 迁移必要的公共能力，但禁止共享 Mapper、Entity 和业务 Service：
   - 统一响应；
   - 错误编码和异常处理；
   - JWT 基础配置；
   - MyBatis-Plus 配置；
   - 日志和健康检查。
4. `account-service` 继续连接现有 `spring` 数据库和 Redis。
5. 约定 `account-service` 是 `user`、`user_address` 以及 `auth:*` Key 的唯一写入者。
6. 保持现有公开 API 路径和响应结构。
7. 使用当前 JWT Secret 签发 Token，并验证 Gateway、原单体和账户服务对同一 Token 的解析一致。
8. Gateway 切换路由：

```text
/api/auth/**        -> lb://account-service
/api/admin/auth/**  -> lb://account-service
/api/addresses/**   -> lb://account-service
其他 /api/**        -> lb://spring-java-service
```

9. 原单体中的认证和地址 Controller 停止对外提供，避免重复处理同一路径；代码可先通过配置关闭，稳定后再删除。
10. 第一小步允许订单暂时只读 `user`、`user_address`，但不得继续写入账户数据。

#### 验收标准

- 注册、普通用户登录、管理员登录、退出和登录限流通过。
- 地址增删改查和默认地址事务通过。
- 账户服务和原单体都能识别账户服务签发的 JWT。
- Nacos 可以看到多个 `account-service` 实例。
- Gateway 能在账户服务实例间负载均衡。
- 原单体不再对外处理账户和地址写接口。
- 停止一个账户服务实例后，认证请求仍可由其他实例处理。

### P3：OpenFeign、LoadBalancer 和 Sentinel

#### 目标

消除原单体对账户表的直接读取，并通过可观察的故障实验学习同步服务调用、超时、重试、熔断和降级。

#### 主要任务

1. 创建 `feature/ms-p3-service-governance`。
2. `account-service` 提供内部接口：

```text
GET /internal/users/{userId}/summary
GET /internal/users/{userId}/addresses/{addressId}
```

3. 返回稳定的内部 DTO，例如 `UserSummary`、`AddressSnapshot`，不暴露持久化 Entity。
4. 原单体使用 OpenFeign 按服务名调用 `account-service`。
5. 通过 Feign RequestInterceptor 传递认证和 Request ID。
6. 配置连接超时、读取超时和日志级别。
7. 地址快照远程查询放在订单本地数据库事务开始之前。
8. 替换订单模块中的 `UserMapper` 和 `AddressService` 跨边界调用。
9. 引入 Sentinel Dashboard，并为 Gateway 和 Feign 调用设计：
   - QPS 限流；
   - 并发线程数限制；
   - 慢调用比例熔断；
   - 异常比例熔断；
   - 自定义 BlockHandler 和 Fallback。
10. 将 Sentinel 规则持久化到 Nacos，避免应用重启后丢失。
11. 完成故障实验：
   - 停止全部账户实例；
   - 人为增加接口延迟；
   - 制造超过阈值的 QPS；
   - 观察熔断打开、半开和恢复。

#### 验收标准

- 原单体不再直接读取 `user`、`user_address`。
- OpenFeign 能通过 Nacos 服务名发现并调用账户服务。
- 多个账户实例之间存在客户端负载均衡。
- 超时、限流、熔断和业务错误能够被区分。
- 写请求不进行无条件自动重试。
- Sentinel 规则可以从 Nacos 恢复。
- 历史订单仍依靠订单快照展示，不依赖账户服务在线。

### P4：Cart Service

#### 目标

迁移购物车接口和 Redis 数据所有权，学习无 MySQL 微服务、跨服务批量查询和依赖降级。

#### 主要任务

1. 创建 `cart-service` 并注册到 Nacos。
2. 迁移购物车增删改查、选中状态和金额展示。
3. `cart:{userId}` 仅允许 `cart-service` 写入。
4. 通过 JWT 本地验签取得用户 ID，不接受前端直接指定用户 ID。
5. 在原单体商品模块先提供内部批量商品查询接口。
6. `cart-service` 使用 OpenFeign 批量取得商品名称、图片、价格、状态和库存，禁止 N+1 调用。
7. 为商品查询配置 Sentinel 超时、熔断和降级。
8. 区分“商品不存在”和“商品服务暂时不可用”，远程故障不能损坏购物车原始数据。
9. 提供按用户和商品 ID 列表幂等清理购物车的内部接口。
10. Gateway 将购物车路径切换到 `cart-service`。

#### 验收标准

- 原有 Redis 购物车数据可以继续使用。
- 商品下架、涨价和库存不足能正确反映到购物车。
- 商品服务超时不会把商品永久标记为已删除。
- 多个购物车实例可以共享 Redis 并正常处理请求。
- 重复清理购物车不会产生错误副作用。

### P5：Product Service

#### 目标

迁移分类、商品、库存、缓存和库存日志，学习共享数据库单写边界、缓存一致性和跨服务幂等写接口。

#### 主要任务

1. 创建 `product-service` 并注册到 Nacos。
2. 迁移 `category`、`product`、`stock` 模块及相关接口。
3. `product-service` 成为 `category`、`product`、`stock_log` 的唯一写入者。
4. 迁移商品缓存和缓存锁的 Redis Key 所有权。
5. 将 Cart 使用的内部商品查询从原单体切换到 `product-service`。
6. 提供批量商品报价接口，返回商品快照、当前价格、状态和可售库存。
7. 提供按 `orderNo` 幂等的库存能力：
   - 扣减/预占库存；
   - 恢复/释放库存；
   - 查询业务处理结果。
8. 新增库存业务幂等记录。不能只依靠 `stock_log.business_no`，因为一个订单可能产生多条商品日志。
9. 为库存接口配置合理的超时和 Sentinel 保护，但不能通过普通降级结果伪造扣减成功。
10. Gateway 将分类、商品和库存管理路径切换到 `product-service`。

#### 验收标准

- 用户端和管理端分类、商品、库存接口通过。
- 并发扣减不会超卖。
- 相同 `orderNo` 重复扣减不会重复减少库存。
- 相同 `orderNo` 重复恢复不会重复增加库存。
- 调用方超时后可以查询库存请求最终结果。
- 原单体停止写商品相关表和商品缓存。

### P6：Order Service、RocketMQ、Outbox 和 Saga

#### 目标

迁移订单全流程，通过订单和库存协作系统学习消息队列、可靠事件、最终一致性、幂等消费和 Saga 补偿。

#### 主要任务

1. 创建 `order-service` 并注册到 Nacos。
2. 迁移订单创建、查询、支付、取消、超时、发货和确认收货。
3. `order-service` 成为所有订单表及订单 Redis Key 的唯一写入者。
4. 部署 RocketMQ NameServer、Broker 和管理控制台。
5. 在订单数据库增加 `outbox_event`，业务状态和待发送事件在同一本地事务中提交。
6. 实现 Outbox 发布任务、发送状态、退避重试和失败告警。
7. 实现消费幂等记录、重复消息测试、失败重试和死信处理。
8. 设计并发布事件：
   - `OrderCreated`
   - `OrderPaid`
   - `OrderCancelled`
   - `CartClearRequested`
   - `OrderTimeoutScheduled`
9. 创建订单流程改造成 Saga：

```text
查询购物车、地址和商品报价
        |
        v
本地创建 PENDING_STOCK 订单及快照
        |
        v
product-service 按 orderNo 幂等扣减库存
        |
        |-- 成功 --> PENDING_PAYMENT + Outbox 事件
        `-- 失败 --> CREATE_FAILED/CANCELLED
```

10. 增加必要的中间状态，例如 `PENDING_STOCK`、`CANCELLING` 和可选 `CREATE_FAILED`。
11. 库存扣减成功但订单更新失败时，执行幂等恢复并由补偿任务持续核对。
12. 支付通过 `OrderPaid` 事件更新商品销量。
13. 下单成功通过 `CartClearRequested` 幂等清理购物车。
14. 订单超时仍以 MySQL 为最终依据，Redis 和消息失败时由补偿任务兜底。
15. Gateway 将订单路径切换到 `order-service`，原单体退出业务流量。

#### 验收标准

- 订单全流程通过。
- 重复下单不会重复创建订单或扣库存。
- 支付和取消竞争只能有一个成功状态。
- 主动取消与超时取消并发时库存只恢复一次。
- 重复 RocketMQ 消息不会重复增加销量或清理非目标购物车数据。
- Broker 暂停后 Outbox 事件能够在恢复后继续发送。
- 任意远程调用超时后，系统最终可以通过查询、重试或补偿达到一致状态。
- 历史订单查询不依赖账户、购物车和商品服务在线。

### P7：Seata、独立 Schema、链路追踪和故障演练

#### 目标

在主流程已经采用 Saga 后，通过对比实验理解不同分布式事务方案和完整微服务可观测性。

#### 主要任务

1. 创建独立实验分支，不直接替换已验证的 Saga 主流程。
2. 使用 Seata AT 模式实现一次订单与库存跨服务事务实验。
3. 选择一个小流程实现 TCC，理解 Try、Confirm、Cancel 和空回滚、防悬挂、幂等。
4. 对比：
   - 普通 `@Transactional`；
   - Seata AT；
   - TCC；
   - Saga + Outbox + 补偿。
5. 在同一 MySQL 实例建立独立 Schema，模拟每服务独立数据库并删除跨服务外键。
6. 引入 Micrometer Tracing，在 Gateway、Feign 和 RocketMQ 链路中传递 Trace ID。
7. 建立统一日志、指标和告警视图。
8. 执行故障演练：
   - Nacos 暂停；
   - 单服务全部实例下线；
   - Feign 超时；
   - Sentinel 熔断；
   - Redis 数据丢失；
   - RocketMQ 重复消息和积压；
   - Outbox 发布进程停止；
   - 数据库事务回滚。
9. 记录每种故障的现象、恢复路径和数据一致性结果。

#### 验收标准

- 能解释并演示普通本地事务为什么不能跨 HTTP 生效。
- 能比较 Seata 和 Saga 的适用条件及代价。
- 可以用 Trace ID 串联 Gateway、Feign、业务服务和消息消费日志。
- 单个组件故障不会造成无法解释的数据状态。
- 独立 Schema 实验完成后，服务不再依赖跨库外键。

## 8. 服务调用和事件契约

### 8.1 同步接口

| 提供方 | 能力 | 调用方 |
| --- | --- | --- |
| `account-service` | 查询用户摘要、查询用户拥有的地址快照 | `order-service`、拆分期间的原单体 |
| `product-service` | 批量商品展示、批量报价 | `cart-service`、`order-service` |
| `product-service` | 按订单号扣减、恢复、查询库存处理结果 | `order-service` |
| `cart-service` | 查询选中商品、幂等清理指定商品 | `order-service` |

同步接口约束：

- 不暴露 Entity、Mapper 和数据库字段全集。
- 写接口必须有幂等键。
- 明确连接和读取超时。
- 只有幂等写操作才允许有限自动重试。
- 业务失败、限流、熔断、超时和服务不存在必须返回可区分结果。

### 8.2 异步事件

| 事件 | 发布方 | 消费方 | 用途 |
| --- | --- | --- | --- |
| `OrderCreated` | `order-service` | 审计或后续扩展服务 | 记录订单创建事实 |
| `OrderPaid` | `order-service` | `product-service` | 幂等增加销量 |
| `OrderCancelled` | `order-service` | 审计或后续扩展服务 | 记录订单取消事实 |
| `CartClearRequested` | `order-service` | `cart-service` | 幂等清理已购买商品 |
| `OrderTimeoutScheduled` | `order-service` | 订单超时消费者 | 可靠注册超时任务 |

事件必须包含：

- 唯一事件 ID；
- 事件类型和版本；
- 业务主键；
- 发生时间；
- Trace ID；
- 幂等消费所需字段。

## 9. 事务边界

### 9.1 保持本地强一致

| 服务 | 本地事务范围 |
| --- | --- |
| `account-service` | 用户注册、地址修改、默认地址切换 |
| `product-service` | 商品与分类修改、单次库存扣减/恢复、库存日志和库存幂等记录 |
| `order-service` | 订单主表、明细、地址快照、操作日志、状态更新和 Outbox |

### 9.2 改为最终一致

| 流程 | 单体实现 | 微服务实现 |
| --- | --- | --- |
| 创建订单和扣库存 | 一个 MySQL 事务 | Saga + 库存幂等 + 失败补偿 |
| 取消订单和恢复库存 | 一个 MySQL 事务 | 中间状态/Outbox + 幂等恢复 |
| 支付和增加销量 | 订单事务直接修改商品 | `OrderPaid` 事件 + 幂等消费 |
| 下单后清购物车 | 提交后 Redis 回调 | Outbox + `CartClearRequested` |
| 注册订单超时 | 提交后写 Redis | Outbox/消息 + MySQL 补偿扫描 |

即使第一轮共享同一个数据库，只要不同服务通过 HTTP 或消息协作，普通 `@Transactional` 就不能跨进程覆盖整个流程。

## 10. 分支和提交策略

长期分支：

```text
main           # 保留模块化单体
microservices  # 微服务长期集成主线
```

阶段分支：

```text
feature/ms-p0-environment
feature/ms-p1-gateway
feature/ms-p2-account
feature/ms-p3-service-governance
feature/ms-p4-cart
feature/ms-p5-product
feature/ms-p6-order
feature/ms-p7-distributed-lab
```

规则：

- 阶段分支从最新 `microservices` 创建。
- 每个阶段独立验收后合并回 `microservices`。
- 不把 `microservices` 整体合并回 `main`。
- 单体安全修复按需使用 `cherry-pick` 同步到另一条线。
- 每阶段完成后打标签，例如 `ms-p1-gateway-complete`。

## 11. 回退策略

- 每次只切换一组 Gateway 路由。
- Gateway 保留回原单体固定地址的备用配置。
- 新服务切换前，原单体旧接口先通过配置关闭，不立即删除代码。
- 共享数据库阶段禁止新旧应用同时写同一业务表。
- 查询路由可快速切回原单体；写路由回退前必须确认唯一写入者。
- RocketMQ、Outbox 和 Saga 上线前准备按 `orderNo` 的订单、库存和事件对账能力。

## 12. 总体验收标准

完成全部阶段后应满足：

- 前端只访问 Gateway。
- 所有服务注册到 Nacos，并从 Nacos 获取约定的配置。
- Gateway 可以按服务名发现实例并负载均衡。
- OpenFeign 调用具有超时、日志、负载均衡和 Sentinel 保护。
- 共享数据库中每张表只有一个逻辑写入服务。
- Redis Key 有明确所有者。
- 不存在跨服务 Mapper 和 Entity 依赖。
- 用户端和管理端全流程通过。
- 订单、库存、支付、取消和发货保持并发与幂等保证。
- RocketMQ 重复、延迟和暂时不可用不会产生重复副作用或永久丢失业务事件。
- 可以演示并解释 Saga、Outbox、Seata 的差异。
- 可以通过 Trace ID 串联 Gateway、Feign、服务和消息链路。
- 能够完成服务下线、超时、熔断、消息积压和缓存丢失等故障演练。

## 13. 当前下一步

P0 环境基线和 P1 Gateway + Nacos 已完成。当前应从最新 `microservices` 创建 `feature/ms-p2-account`，开始抽取 Account Service。

P2 第一批具体任务：

```text
1. 从 microservices 创建 feature/ms-p2-account
2. 创建 account-service Maven 模块和 account-service.yaml
3. 明确 auth、user、address 的数据所有权和共享数据库边界
4. 迁移账户领域代码，禁止跨服务共享 Mapper、Entity 和业务 Service
5. 验证 JWT 能在 Gateway、account-service 和保留单体之间传递
6. 将账户相关 Gateway 路由从 spring-java-service 切换到 account-service
7. 完成新旧路由回退、登录注册和地址管理回归
```
