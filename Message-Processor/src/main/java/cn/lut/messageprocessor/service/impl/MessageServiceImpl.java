package cn.lut.messageprocessor.service.impl;

import cn.lut.messageprocessor.entity.Message;
import cn.lut.messageprocessor.mapper.MessageMapper;
import cn.lut.messageprocessor.service.MessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
    @Override
    public void removeMessageWithConversationId(String conversationId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, Long.parseLong(conversationId));
        this.remove(wrapper);
    }
}