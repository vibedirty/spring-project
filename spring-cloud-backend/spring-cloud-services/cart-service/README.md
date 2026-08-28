# Cart Service (P4 购物车微服务)

## 1. 模块职责
- 拥有 `cart:{userId}` Redis Key 的**唯一读写所有权**（纯 Redis 微服务，无 MySQL 依赖）。
- 提供用户端购物车增删改查、选中状态维护与金额动态计算。
- 提供内部接口（`/internal/cart/selected-items`、`/internal/cart/clear-items`）供订单等服务跨服务调用。
- 通过 OpenFeign 批量查询商品信息（防 N+1），集成 Sentinel 超时、限流与熔断降级。

## 2. 核心端口与配置
- 默认端口：`8102`
- Nacos Group：`SERVICE_GROUP`（DataId: `cart-service.yaml`）
- Sentinel 规则 Group：`SENTINEL_GROUP`（`cart-service-sentinel-flow.json` / `cart-service-sentinel-degrade.json`）

## 3. 本地启动示例
```bash
# 单实例启动
SERVER_PORT=8102 java -jar target/cart-service-0.0.1-SNAPSHOT.jar

# 多实例启动（验证 Redis 数据共享与网关负载均衡）
SERVER_PORT=8103 java -jar target/cart-service-0.0.1-SNAPSHOT.jar
```
