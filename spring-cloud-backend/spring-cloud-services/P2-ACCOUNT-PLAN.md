# P2 Account Service 简要实施计划

## 目标

从 `spring-java-service` 中抽取 `auth`、`user`、`address`，建立第一个独立业务微服务 `account-service`。对外 API 路径和响应结构保持不变，前端仍只访问 Gateway。

P2 继续共享现有 MySQL 和 Redis，重点验证服务边界、JWT 跨服务使用、Nacos 注册配置和 Gateway 路由切换；OpenFeign、Sentinel 和独立数据库留到后续阶段。

## 边界约定

- `account-service` 成为 `user`、`user_address` 表的唯一写入者。
- `account-service` 拥有 `auth:jwt:session:*`、`auth:login:failures:*` Redis Key。
- 原单体在 P2 可以临时只读账户数据，P3 再通过服务调用消除直接读取。
- 不共享 Mapper、Entity 和业务 Service；只迁移必要的统一响应、异常、JWT、安全、日志和数据访问配置。
- JWT Secret、数据库密码等敏感值继续使用环境变量，不写入 Nacos。

## 实施步骤

### 1. 建立模块和配置

- 从最新 `microservices` 创建 `feature/ms-p2-account`。
- 在 `spring-cloud-services` 下创建 `account-service` Maven 模块，默认端口 `8101`。
- 接入 WebMVC、Validation、Security、Actuator、Nacos Discovery/Config、MyBatis-Plus、MySQL、Redis 和 JWT。
- 创建 `infra/nacos/config/account-service.yaml`，并扩展 `infra/nacos/bootstrap.sh` 发布到 `hard-dev`、`hard-test`。
- 启动空服务，确认 Nacos 中出现 `HARD_GROUP/account-service`。

### 2. 迁移账户领域

- 迁移 `auth`、`user`、`address` 包及对应测试。
- 保持现有公开接口不变：

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
POST /api/admin/auth/login
POST /api/admin/auth/logout
/api/addresses/**
```

- 复用现有 `user`、`user_address` 表，不在 P2 改表或拆 Schema。
- 验证注册、登录限流、Token 会话、退出和地址默认值事务。

### 3. 保持跨服务 JWT 一致

- `account-service` 负责签发 Token。
- `account-service` 和原单体使用相同的 JWT Secret、算法、Claims 和 Redis 会话规则。
- Gateway 继续原样转发 `Authorization` 和 `X-Request-ID`，不承担业务鉴权。
- 验证账户服务签发的 Token 可以访问原单体中的购物车和订单接口。

### 4. 切换 Gateway 路由

在 `gateway-service.yaml` 中将账户相关路径放在通用 `/api/**` 路由之前：

```text
/api/auth/**        -> lb://account-service
/api/admin/auth/**  -> lb://account-service
/api/addresses/**   -> lb://account-service
其他 /api/**        -> lb://spring-java-service
```

发布 Nacos 配置后验证 Gateway 动态刷新。原单体账户 Controller 先通过配置关闭对外入口，稳定后再删除，避免新旧服务同时处理账户写请求。

### 5. 多实例、故障和回退验证

- 启动两个不同端口的 `account-service` 实例，确认负载均衡。
- 停止其中一个实例，确认登录和地址请求继续成功。
- 停止全部账户实例，确认错误响应明确且不会错误回落到原单体写入。
- 准备固定地址或原单体路由回退方案；回退前确认只有一个账户写入者。

## 验收清单

- [ ] `account-service` 能构建、测试并注册到 Nacos。
- [ ] 注册、普通登录、管理员登录、退出和登录限流通过。
- [ ] 地址增删改查和默认地址事务通过。
- [ ] 账户服务签发的 JWT 能被原单体正确解析。
- [ ] Gateway 按路径将账户请求转发到 `account-service`。
- [ ] 两个账户实例可以负载均衡，一个实例下线不影响整体服务。
- [ ] `user`、`user_address` 和账户 Redis Key 只有 `account-service` 写入。
- [ ] 用户端、管理端和原单体其他业务回归通过。
- [ ] 路由可以安全回退，且不会出现新旧应用双写。

## 建议提交顺序

```text
1. feat: scaffold account service and Nacos config
2. feat: migrate authentication and user domain
3. feat: migrate address domain
4. feat: route account APIs through account service
5. test: verify account failover and rollback
```
