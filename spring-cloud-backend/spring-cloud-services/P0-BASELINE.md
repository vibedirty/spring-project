# P0 环境与版本验证基线

验证日期：2026-08-27
验证分支：`feature/ms-p0-environment`

## 本机工具与基础设施

| 项目 | 验证值 |
| --- | --- |
| JDK | OpenJDK 17.0.15 |
| Maven Wrapper | 3.9.16 |
| Docker | 29.1.3 |
| Docker Compose | v5.0.0-desktop.1 |
| MySQL | 现有本机容器 `mysql-common`，MySQL 8.0 |
| Redis | 现有本机容器 `redis-common`，Redis 8.2.8 Alpine |
| Nacos | `nacos/nacos-server:v3.1.1`，standalone + embedded storage |

MySQL 和 Redis 继续沿用项目已有的本地环境；P0 新增的 Compose 只管理 Nacos。

## Maven 与源码验证

统一版本：

| 组件 | 版本 |
| --- | --- |
| Spring Boot | 4.0.0 |
| Spring Cloud | 2025.1.0 |
| Spring Cloud Alibaba | 2025.1.0.0 |
| Java 编译目标 | 17 |

已验证：

- 根 Reactor 能识别 `spring-java` 和 `spring-cloud-services`；
- Maven Enforcer 的 JDK 17、Maven 3.9.x 规则通过；
- 139 个主源码文件在 `--release 17` 下重新编译成功；
- 79 个测试源码文件重新编译成功；
- Spring Boot 4.0.0 可执行 JAR 打包成功；
- 可执行 JAR 已使用宿主机 JDK 17 在 `18080` 端口启动，`/actuator/health` 返回 `UP`，其中 MySQL、Redis 均为 `UP`；验收后已关闭临时进程。

验证命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd spring-cloud-backend
./spring-java/mvnw clean package -DskipTests
```

## 回归测试基线

完整测试运行结果：

```text
Tests run: 357
Failures: 0
Errors: 1
Skipped: 0
```

唯一错误是拆分计划已接受记录的订单幂等 TTL 约定差异：

- `OrderIdempotencyService` 当前实现：5 分钟；
- `OrderIdempotencyServiceTests` 当前期望：24 小时；
- Mockito 严格桩因此报告 `PotentialStubbingProblem`。

P0 不擅自改变订单幂等业务语义。该差异不是 Spring Boot 4.0.0 编译或启动兼容问题，留待独立业务决策统一实现和测试约定。

## Nacos 验证

- Compose 渲染通过；
- 镜像固定为 Nacos 3.1.1，没有使用 `latest`；
- API `8848`、Console `8849`、gRPC `9848` 均只绑定 `127.0.0.1`；
- Nacos 日志确认以 standalone、embedded storage 模式启动；
- Nacos Server API 3.1.1 与 Console 3.1.1 启动成功；
- Console 首页返回 HTTP 200；
- `hard-dev`、`hard-test` 及 P0 标记配置可通过 `bootstrap.sh` 重建。
- `hard-dev / COMMON_GROUP / common.yaml` 在 Nacos 容器重启前后内容一致，MD5 均为 `da0298442b884017184f5470eb0cbbf5`，Volume 持久化验证通过。
