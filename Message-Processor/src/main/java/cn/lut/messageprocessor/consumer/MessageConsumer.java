package cn.lut.messageprocessor.consumer;

import cn.lut.messageprocessor.entity.Message;
import cn.lut.messageprocessor.service.ConversationService;
import cn.lut.messageprocessor.service.MessageService;
import com.alibaba.fastjson2.JSON;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class MessageConsumer {
    private final Logger logger = Logger.getLogger(MessageConsumer.class.getName());

    @Autowired
    private MessageService messageService;
    @Autowired
    private ConversationService conversationService;

    @KafkaListener(topics = "save-message", groupId = "message-processor-group")
    public void saveMessageConsume(ConsumerRecord<String, String> record) {
        logger.info("Received message from Kafka topic 'save-message': " + record.value());
        String messageJson = record.value();
        Message message = JSON.parseObject(messageJson, Message.class);

        logger.info(message.toString());

        // 持久化到数据库
        messageService.save(message);
        // 更新会话的最后一条消息 ID
        conversationService.updateLastMessageId(message.getConversationId(), message.getMessageId());
    }
}