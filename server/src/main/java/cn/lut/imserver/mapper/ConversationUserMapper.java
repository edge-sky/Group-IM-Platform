package cn.lut.imserver.mapper;

import cn.lut.imserver.entity.ConversationUser;
import cn.lut.imserver.entity.vo.UserVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface ConversationUserMapper extends BaseMapper<ConversationUser> {
    Integer insertBatch(@Param("list") List<ConversationUser> list);

    int existsByConversationIdAndUid(@Param("conversationId") long conversationId, @Param("uid") long uid);

    int isOperatePermission(@Param("uid") long uid, @Param("conversationId") long conversationId, @Param("memberPermission") boolean memberPermission, @Param("messagePermission") boolean messagePermission, @Param("fileVisiblePermission") boolean fileVisiblePermission, @Param("fileOperatePermission") boolean fileOperatePermission);

    List<UserVo> getUserVoByConversationId(long conversationId);

    int removeUserFromConversation(@Param("conversationId")long conversationId, @Param("userIdToRemove")long userIdToRemove);

    ConversationUser getConversationUserByUid(@Param("conversationId")long conversationId, @Param("uid")long uid);

    /**
     * 插入或更新会话用户记录。
     * 如果用户-会话组合已存在，则将其 deleted 状态更新为 0。
     *
     * @param conversationUser 要插入或更新的会话用户对象
     */
    int insertOrUpdateUserConv(ConversationUser conversationUser);
}