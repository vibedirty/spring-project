# Spring Cloud Services

该目录是微服务模块的 Maven 聚合目录。P0 只建立父工程和版本基线，不提前创建空的 Spring Boot 应用。

后续阶段按需增加模块：

| 阶段 | 模块 |
| --- | --- |
| P1 | `gateway-service` |
| P2 | `account-service` |
| P4 | `cart-service` |
| P5 | `product-service` |
| P6 | `order-service` |

所有模块必须继承 `spring-cloud-backend/pom.xml` 中的 `spring-project-parent`，不得自行覆盖 Spring Cloud、Spring Cloud Alibaba、Nacos、Sentinel、RocketMQ 或 Seata 的版本。仓库根目录不是 Maven 工程，前后端项目仍保持彼此独立。

## P0 版本基线

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
