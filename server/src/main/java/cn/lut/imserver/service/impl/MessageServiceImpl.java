package cn.lut.imserver.service.impl;

import cn.lut.imserver.entity.Message;
import cn.lut.imserver.entity.vo.MessageVo;
import cn.lut.imserver.service.MessageService;
import cn.lut.imserver.util.RedisUtil;
import cn.lut.imserver.util.MqUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.lut.imserver.mapper.MessageMapper;

import java.util.List;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {
    @Autowired
    private MqUtil mqUtil;
    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void removeMessageWithConversationId(String conversationId) {
        messageMapper.removeMessageWithConversationId(conversationId);
    }

    @Override
    public List<MessageVo> getMessageVoWithLimit(long conversationId, long earliestMessageId, int limit) {
        return messageMapper.getMessageVoWithLimit(conversationId, earliestMessageId, limit);
    }
}