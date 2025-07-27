package cn.lut.imserver.consumer;

import cn.lut.imserver.entity.Message;
import cn.lut.imserver.entity.vo.MessageVo;
import cn.lut.imserver.handle.WebSocketHandle;
import cn.lut.imserver.service.ConversationService;
import cn.lut.imserver.service.MessageService;
import cn.lut.imserver.util.RedisUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

@Component
@Slf4j
public class MessageConsumer {
    @Autowired
    private MessageService messageService;
    @Autowired
    private ConversationService convService;
    @Autowired
    ConversationService conversationService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private WebSocketHandle webSocketHandle;

    @KafkaListener(topics = "save-message", groupId = "im-server-group")
    public void saveMessageConsume(ConsumerRecord<String, String> record) throws IOException {
        log.info("Received message from Kafka topic 'save-message': {}", record.value());
        String messageJson = record.value();
        Message message = JSON.parseObject(messageJson, Message.class);

        // 防止重复消费，保证幂等性
        LambdaQueryWrapper<Message> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Message::getConversationId, message.getConversationId())
                .eq(Message::getMessageId, message.getMessageId());
        if (messageService.exists(lambdaQueryWrapper)) {
            log.warn("Message with ID {} already exists in conversation {}", message.getMessageId(), message.getConversationId());
            return; // 如果消息已存在，则不进行保存
        }

        MessageVo messageVo = new MessageVo();
        messageVo.setConversationId(String.valueOf(message.getConversationId()));
        messageVo.setMessageId(message.getMessageId());
        messageVo.setContent(message.getContent());
        messageVo.setType(message.getType());
        messageVo.setFromUid(String.valueOf(message.getFromUid()));
        messageVo.setTime(message.getTime());
        messageVo.setWithdrawn(message.getWithdrawn());
        messageVo.setIsRead(message.getIsRead());

        // 持久化到数据库
        messageService.save(message);
        // 更新会话的最后一条消息 ID
        conversationService.updateLastMessageId(message.getConversationId(), message.getMessageId());
        // 将消息写入 Redis 缓存
        redisUtil.addMessageToConversationCache(messageVo, 7);

        // 推送消息到 WebSocket 客户端
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(messageVo));
        jsonObject.put("respond", "receiveMessage");
        Set<Long> uidList = redisUtil.getConvUidList(message.getConversationId());
        webSocketHandle.pushJsonObjectToMultiClient(uidList, jsonObject);
    }

    @KafkaListener(topics = "DeleteMessageWithConversationId", groupId = "im-server-group")
    public void deleteMessageWithConversationIdConsume(ConsumerRecord<String, String> record) {
        String conversationIdStr = record.value();
        long conversationId = Long.parseLong(conversationIdStr);
        // 删除与会话相关的所有消息
        messageService.removeMessageWithConversationId(conversationIdStr);
        // 清除 Redis 中的相关消息缓存
        String redisKeyPattern = "conv_msgs:" + conversationId; // Adjusted to match the new key prefix
        redisUtil.delete(redisKeyPattern); // Assuming delete can handle a pattern or specific key
    }

    // TODO: 删除已删除的文件的文件版本
}