# Spring Cloud Services

该目录是微服务模块的 Maven 聚合目录。P0 建立父工程和版本基线，P1 开始按阶段创建可运行服务，不提前创建后续阶段的空应用。

后续阶段按需增加模块：

| 阶段 | 模块 |
| --- | --- |
| P1 | `gateway-service` |
| P2 | `account-service` |
| P3 | 在现有三个服务中增加 OpenFeign、LoadBalancer 与 Sentinel 治理 |
| P4 | `cart-service` |
| P5 | `product-service` |
| P6 | `order-service` |

当前已完成 P1、P2、P3。`gateway-service` 负责统一入口、路由、跨域、请求标识和路由级限流；`account-service` 拥有账户数据并提供内部快照契约；`spring-java-service` 通过 OpenFeign 按服务名读取账户快照，不再从订单域直读账户表。P3 的启动与故障实验见 [P3 服务治理说明](../../../docs/p3-service-governance.md)。

所有模块必须继承 `spring-cloud-backend/pom.xml` 中的 `spring-project-parent`，不得自行覆盖 Spring Cloud、Spring Cloud Alibaba、Nacos、Sentinel、RocketMQ 或 Seata 的版本。仓库根目录不是 Maven 工程，前后端项目仍保持彼此独立。

## P0/P1 版本基线

| 组件 | 版本 |
| --- | --- |
| JDK | 17 |
| Spring Boot | 4.0.0 |
| Spring Cloud | 2025.1.0 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Nacos Client | 由 Spring Cloud Alibaba BOM 管理 |

在 macOS 上构建前先确保 Maven 运行在 JDK 17：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd spring-cloud-backend
./spring-java/mvnw clean verify
```

`spring-cloud-backend/pom.xml` 使用 Maven Enforcer 检查实际运行的 Java 和 Maven 版本。不满足 JDK 17 或 Maven 3.9.x 时会快速失败。
