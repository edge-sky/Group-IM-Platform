package cn.lut.imserver.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@EnableAsync
@Slf4j
public class MqUtil {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Async
    public void sendMessage(String topic, String key, String message) {
        log.info("Sending message to Kafka topic '" + topic + "': " + message);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, message);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.warn("Message send fail with {}", ex.getMessage());
                try {
                    retry(topic, key, message);
                } catch (InterruptedException e) {
                    log.warn("Retry fail");
                    throw new RuntimeException(e);
                }
            } else {
                log.info("Message send success {}", result.getRecordMetadata().toString());
            }
        });
    }

    private void retry(String topic, String key, String message) throws InterruptedException {
        int retryTimes = 5;
        while (retryTimes > 0) {
            Thread.sleep(1000);
            try {
                kafkaTemplate.send(topic, key, message).get();
                log.info("Retry success");
                return;
            } catch (Exception ex) {
                log.info("Retry false, sleep 1s");
            }
            retryTimes--;
        }
    }
}
