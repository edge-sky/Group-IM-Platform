# Message-Processor 服务

## 项目说明

这是从IM-Server项目中分离出来的Kafka消息落盘处理模块，作为一个独立的微服务运行。

## 功能职责

1. 接收并处理Kafka消息
2. 将消息持久化到数据库
3. 更新会话的最后一条消息ID
4. 将消息写入Redis缓存
5. 将消息转发到WebSocket推送主题

## 与IM-Server的协作方式

1. IM-Server发送消息到`save-message`主题
2. Message-Processor处理消息并持久化
3. Message-Processor将消息转发到`push-message`主题
4. IM-Server监听`push-message`主题并推送消息到WebSocket客户端

## 配置说明

- 服务端口：8081（与IM-Server区分）
- 消费者组ID：message-processor-group
- 监听主题：save-message, DeleteMessageWithConversationId
- 发送主题：push-message

## 启动方式

```bash
mvn spring-boot:run
```

确保Kafka、MySQL和Redis服务已经启动。