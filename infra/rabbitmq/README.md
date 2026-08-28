# RabbitMQ 基础设施说明

## 1. 概述
P6 阶段引入 RabbitMQ 作为微服务异步事件通知与延时关单中间件。

## 2. 启动与管理
在宿主机执行以下命令启动 RabbitMQ 服务：
```bash
docker compose -f infra/rabbitmq/compose.yaml up -d
```

## 3. 访问端口
- AMQP 协议端口：`127.0.0.1:5672`
- 管理控制台 UI：`http://127.0.0.1:15672`（默认账号：`guest`，默认密码：`guest`）
