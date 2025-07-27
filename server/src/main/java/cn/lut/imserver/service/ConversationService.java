package cn.lut.imserver.service;

import cn.lut.imserver.entity.Conversation;
import cn.lut.imserver.entity.vo.ConversationVo;
import cn.lut.imserver.entity.vo.UserVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ConversationService extends IService<Conversation> {
    boolean createConversation(Conversation conversation);

    boolean addUsersToConversation(long conversationId, long uid);

    boolean deleteConversation(Long conversationId);

    int existsByConversationIdAndUid(long conversationId, long uid);

    boolean updateLastMessageId(long conversationId, long lastMessageId);

    // 判断用户是否有操作权限
    boolean isOperatePermission(long uid, long conversationId, boolean memberOperation, boolean messagePermission, boolean fileVisiblePermission, boolean fileOperatePermission);

    List<ConversationVo> getConversationsByUserId(long uid);

    List<Long> getUserIdsByConversationId(long conversationId);

    boolean isUserInConversation(long uid, long conversationId);

    List<UserVo> getUserVoByConversationId(long conversationId);

    boolean getMemberPermission(long uid, long conversationId);

    boolean removeUserFromConversation(long conversationId, long userIdToRemove);

    boolean updateMemberPermission(long conversationId, long targetUid, String permission, boolean value);
}
