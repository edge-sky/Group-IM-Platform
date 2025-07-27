package cn.lut.imserver.service;

import cn.lut.imserver.entity.Message;
import cn.lut.imserver.entity.vo.MessageVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;


public interface MessageService extends IService<Message> {
    void removeMessageWithConversationId(String conversationId);

    List<MessageVo> getMessageVoWithLimit(long conversationId, long earliestMessageId, int limit);
}
