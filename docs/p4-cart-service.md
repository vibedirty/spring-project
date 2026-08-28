# P4 Cart Service 改造与服务治理演练指南

本文档总结了 P4 阶段（Cart Service 购物车微服务抽取）的架构改造成果、设计原则与演练步骤。

---

## 1. 架构改造全貌

```text
                               ┌────────────────────────────────┐
                               │       Gateway (端口 9000)      │
                               └──────────────┬─────────────────┘
                                              │
                    ┌─────────────────────────┴────────────────────────┐
                    │ /api/cart/**                                     │ /api/orders/**
                    ▼                                                  ▼
     ┌─────────────────────────────┐                    ┌─────────────────────────────┐
     │  cart-service (端口 8102)    │                    │  spring-java (单体, 端口 8080) │
     │  - 纯 Redis 微服务 (无 MySQL) │                    │  - 订单创建与状态结算        │
     │  - 拥有 cart:{userId} 独占权  │                    │  - 提供商品内部批量查询接口  │
     └──────────────┬──────────────┘                    └──────────────┬──────────────┘
                    │                                                  │
                    │ 1. [OpenFeign 批量商品查询, 防 N+1]               │ 2. [OpenFeign 选购项查询]
                    │    GET /internal/products/batch-summary?ids=...   │    GET /internal/cart/selected-items
                    └──────────────────────────────────────────────────┤
                                                                       │ 3. [OpenFeign 幂等已购清理]
                                                                       │    POST /internal/cart/clear-items
                                                                       ▼
```

---

## 2. 核心架构设计

### 2.1 无 MySQL 纯 Redis 微服务
* `cart-service` 不连接关系型数据库，所有购物车条目以 `Hash` 结构存储于 Redis：`cart:{userId}` -> `{productId: CartItemJson}`。
* 独立部署后，多个 `cart-service` 实例无状态运行，天然支持横向扩容。

### 2.2 批量商品查询与防 N+1
* 购物车列表与金额计算时，通过 `productQueryService.getProductSummaries(productIds)` 一次性批量获取商品当前的价格、库存与售卖状态。
* 严禁在循环中进行单个远程商品接口调用。

### 2.3 故障隔离与原始数据保护
* 区分**业务状态（商品下架/删除）**与**基础设施故障（商品服务超时/熔断）**：
  * 商品服务正常但商品下架：返回 `valid=false, invalidReason="商品已下架"`；
  * 商品服务宕机/超时/熔断：捕获 `ProductDependencyException`，降级返回 `valid=false, invalidReason="商品服务暂时不可用"`，**绝不清空或损坏 Redis 中的用户购物车原始数据**。

---

## 3. 验证与演练步骤

### 实验 1：多实例共享与客户端负载均衡
1. 启动两个 `cart-service` 实例：
   ```bash
   SERVER_PORT=8102 java -jar cart-service/target/cart-service-0.0.1-SNAPSHOT.jar
   SERVER_PORT=8103 java -jar cart-service/target/cart-service-0.0.1-SNAPSHOT.jar
   ```
2. 通过网关 `http://localhost:9000/api/cart` 发送加入购物车请求；
3. 两个实例能够共享 Redis 数据并被网关均匀分发。

### 实验 2：依赖降级与购物车数据保护
1. 模拟商品服务故障或停止 `spring-java`；
2. 请求 `http://localhost:9000/api/cart` 获取购物车；
3. 接口正常返回 200，购物车列表包含原始商品，且标记为 `valid=false, invalidReason="商品服务暂时不可用"`；
4. 恢复商品服务后，购物车自动恢复正常展示，用户数据零丢失。
