package cn.lut.imserver.mapper;

import cn.lut.imserver.entity.Conversation;
import cn.lut.imserver.entity.vo.ConversationVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
    int updateLastMessageId(@Param("conversationId") long conversationId, @Param("lastMessageId") long lastMessageId);



    List<ConversationVo> getConversationsByUserId(@Param("uid") long uid);

    List<Long> getUserIdsByConversationId(@Param("conversationId") long conversationId);

    int getMemberPermission(long uid, long conversationId);
}