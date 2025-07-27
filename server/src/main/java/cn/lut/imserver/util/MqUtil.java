package cn.lut.imserver.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
@Slf4j
@EnableAsync
public class MqUtil {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Async
    public void sendMessage(String topic, String message) {
        log.info("Sending message to Kafka topic '{}': {}", topic, message);
        log.info("message size is {} bytes", message.getBytes().length);
        kafkaTemplate.send(topic, message);
    }
}