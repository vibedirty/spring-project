# spring-project

该仓库统一管理用户端、管理端、Java 后端和本地基础设施，各端项目保持独立构建。

```text
spring-project/
├── frontend/                  # React 用户端
├── frontend-admin/            # React 管理端
├── spring-cloud-backend/      # Java 后端 Maven Reactor
│   ├── pom.xml                # 统一父 POM 和聚合入口
│   ├── spring-java/           # 模块化单体
│   └── spring-cloud-services/ # 微服务聚合工程
└── infra/                     # Nacos 等本地基础设施
```

Java 后端聚合构建：

```bash
cd spring-cloud-backend
./spring-java/mvnw clean verify
```
