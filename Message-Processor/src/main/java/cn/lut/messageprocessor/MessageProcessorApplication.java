package cn.lut.messageprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MessageProcessorApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessageProcessorApplication.class, args);
    }
}