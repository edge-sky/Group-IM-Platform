package cn.lut.imserver.mapper;

import cn.lut.imserver.entity.Message;
import cn.lut.imserver.entity.vo.MessageVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    void removeMessageWithConversationId(String conversationId);

    List<MessageVo> getMessageVoWithLimit(long conversationId, long earliestMessageId, int limit);
}
