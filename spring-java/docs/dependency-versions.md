# 002 依赖兼容版本清单

确认日期：2026-08-19

## 锁定组合

| 组件 | 锁定版本 | Maven 使用方式 |
| --- | --- | --- |
| Java | 17 | `<java.version>17</java.version>` |
| Maven Wrapper | 3.9.16 | 继续使用当前 `.mvn/wrapper/maven-wrapper.properties` |
| Spring Boot | 4.1.0 | 继续作为 `spring-boot-starter-parent` |
| Spring Framework | 7.0.8 | 由 Spring Boot 4.1.0 管理，不单独指定 |
| Spring Security | 7.1.0 | 使用 `spring-boot-starter-security`，不单独指定版本 |
| MyBatis-Plus | 3.5.17 | 使用 `mybatis-plus-spring-boot4-starter`，显式指定版本 |
| MyBatis-Spring | 4.0.0 | 由 MyBatis-Plus Boot 4 starter 引入，不单独指定 |
| JJWT | 0.13.0 | `jjwt-api`、`jjwt-impl`、`jjwt-jackson` 使用同一版本 |

## 后续计划依赖

下列依赖也已放入同一个 Maven 模型完成解析，用于提前排除 004～006 的版本组合问题：

| 用途 | 坐标或 starter | 解析版本 | 版本管理方式 |
| --- | --- | --- | --- |
| Spring MVC | `spring-boot-starter-webmvc` | 4.1.0 | Spring Boot |
| 参数校验 | `spring-boot-starter-validation` | 4.1.0 | Spring Boot |
| Redis | `spring-boot-starter-data-redis` | 4.1.0 | Spring Boot |
| MySQL 驱动 | `com.mysql:mysql-connector-j` | 9.7.0 | Spring Boot |
| OpenAPI/Swagger UI | `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 3.0.3 | 显式指定 |

## Maven 声明规则

- Spring Boot、Spring Framework、Spring Security、MySQL 和 Redis 相关依赖采用 Spring Boot 依赖管理，不手写子依赖版本。
- MyBatis-Plus 必须使用 Boot 4 专用的 `mybatis-plus-spring-boot4-starter`，不要使用 Boot 2 或 Boot 3 starter。
- JJWT 使用拆分模块：`jjwt-api` 为默认编译作用域，`jjwt-impl` 和 `jjwt-jackson` 为 `runtime`。
- MyBatis-Plus、JJWT 和 springdoc-openapi 不在 Spring Boot BOM 中，版本应通过项目属性集中锁定。
- 不直接声明 Spring Framework、Spring Security Core、MyBatis 或 MyBatis-Spring，以免绕过 starter 的兼容组合。

## 验证记录

使用临时 Maven 模型同时加入 Web MVC、Validation、Security、Redis、MyBatis-Plus、MySQL、JJWT 和 springdoc-openapi，并执行：

```bash
./mvnw -f .002-compatibility-pom.xml -DskipTests compile dependency:tree
```

验证结果：

- Maven 成功解析所有依赖，结果为 `BUILD SUCCESS`。
- 现有 2 个主代码源文件以 `javac --release 17` 编译成功。
- 实际依赖树落地为 Spring Framework 7.0.8、Spring Security 7.1.0、MyBatis-Plus 3.5.17、MyBatis-Spring 4.0.0 和 JJWT 0.13.0。
- 本机 Maven 当前运行在 JDK 26.0.1；项目的编译目标仍锁定为 Java 17。Spring Boot 4.1.0 官方支持 Java 17 至 Java 26。

临时 Maven 模型仅用于 002 验证，验证后已删除。004～006 已按本清单将正式依赖加入 `pom.xml`。

## 参考依据

- [Spring Boot 4.1.0 系统要求](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot 4.1.0 托管依赖坐标](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html)
- [MyBatis-Plus Boot 4 starter 说明](https://github.com/baomidou/mybatis-plus)
- [MyBatis-Plus 3.5.17 发布记录](https://github.com/baomidou/mybatis-plus/releases)
- [JJWT 安装说明](https://github.com/jwtk/jjwt/blob/main/README.adoc#installation)
- [springdoc-openapi 与 Spring Boot 兼容矩阵](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot)
