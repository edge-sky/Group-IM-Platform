package cn.lut.messageprocessor.service;

import cn.lut.messageprocessor.entity.Message;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MessageService extends IService<Message> {
    void removeMessageWithConversationId(String conversationId);
}