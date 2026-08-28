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
│       └── gateway-service/   # P1 统一 API 入口
└── infra/                     # Nacos 等本地基础设施
```

Java 后端聚合构建：

```bash
cd spring-cloud-backend
./spring-java/mvnw clean verify
```

本地统一启动 Java 服务：

```bash
./run.sh
```

脚本会询问“重新打包后运行”或“直接运行现有 JAR”。所有服务都在当前窗口
临时运行，按 `Ctrl+C` 或关闭窗口就会全部退出。新增微服务时，在脚本顶部的
`ms_services` 数组中追加服务名、端口和 JAR 路径即可。
