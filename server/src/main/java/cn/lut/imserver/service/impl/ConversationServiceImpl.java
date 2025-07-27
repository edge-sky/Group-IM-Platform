package cn.lut.imserver.service.impl;

import cn.lut.imserver.entity.Conversation;
import cn.lut.imserver.entity.vo.ConversationVo;
import cn.lut.imserver.entity.vo.UserVo;
import cn.lut.imserver.mapper.ConversationMapper;
import cn.lut.imserver.service.ConversationService;
import cn.lut.imserver.util.MqUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;
import cn.lut.imserver.entity.ConversationUser;
import cn.lut.imserver.mapper.ConversationUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation> implements ConversationService {
    @Autowired
    private MqUtil mqUtil;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ConversationUserMapper conversationUserMapper;

    // 创建会话
    @Override
    @Transactional
    public boolean createConversation(Conversation conversation) {
        // 插入会话记录
        int inserted = conversationMapper.insert(conversation);
        if (inserted <= 0) {
            return false;
        }

        // 插入会话用户关联记录
        ConversationUser conversationUser = new ConversationUser();
        conversationUser.setConversationId(conversation.getId());
        conversationUser.setUid(conversation.getManagerUid());
        conversationUser.setMemberPermission(1);
        conversationUser.setFileVisiblePermission(1);
        conversationUser.setFileOperatePermission(1);
        conversationUser.setMessagePermission(1);
        conversationUserMapper.insert(conversationUser);
        return true;
    }

    @Override
    @Transactional
    public boolean addUsersToConversation(long conversationId, long uid) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            return false; // 会话不存在
        }

        // 创建一个新的 ConversationUser 对象用于插入或更新
        ConversationUser conversationUser = new ConversationUser();
        conversationUser.setConversationId(conversationId);
        conversationUser.setUid(uid);
        // 调用自定义的 insertOrUpdate 方法
        conversationUserMapper.insertOrUpdateUserConv(conversationUser);

        // 查询会话成员数量 (只计算未删除的)
        QueryWrapper<ConversationUser> countQuery = new QueryWrapper<>();
        countQuery.eq("conversation_id", conversationId).eq("deleted", 0);
        Long currentMemberCount = conversationUserMapper.selectCount(countQuery);
        // 更新会话成员数量
        conversation.setUserNum(currentMemberCount.intValue());
        conversationMapper.updateById(conversation);
        return true;
    }

    @Override
    @Transactional
    public boolean deleteConversation(Long conversationId) {
        // 删除会话记录
        int deletedConversation = conversationMapper.deleteById(conversationId);
        if (deletedConversation <= 0) {
            return false; // 会话不存在或删除失败
        }
        // 删除会话用户关联记录
        QueryWrapper<ConversationUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        conversationUserMapper.delete(queryWrapper);

        // 删除数据库中的信息
        mqUtil.sendMessage("DeleteMessageWithConversationId", conversationId.toString());

        return true;
    }

    @Override
    public int existsByConversationIdAndUid(long conversationId, long uid) {
        return conversationUserMapper.existsByConversationIdAndUid(conversationId, uid);
    }

    @Override
    public boolean updateLastMessageId(long conversationId, long lastMessageId) {
        return conversationMapper.updateLastMessageId(conversationId, lastMessageId) > 0;
    }

    @Override
    public boolean isOperatePermission(long uid,
                                       long conversationId,
                                       boolean memberPermission,
                                       boolean messagePermission,
                                       boolean fileVisiblePermission,
                                       boolean fileOperatePermission) {
        return conversationUserMapper.isOperatePermission(uid, conversationId, memberPermission, messagePermission, fileVisiblePermission, fileOperatePermission) > 0;
    }

    @Override
    public List<ConversationVo> getConversationsByUserId(long uid) {
        return conversationMapper.getConversationsByUserId(uid);
    }

    @Override
    public List<Long> getUserIdsByConversationId(long conversationId) {
        return conversationMapper.getUserIdsByConversationId(conversationId);
    }

    @Override
    public boolean isUserInConversation(long uid, long conversationId) {
        QueryWrapper<ConversationUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", uid).eq("conversation_id", conversationId).eq("deleted", 0);
        long count = conversationUserMapper.selectCount(queryWrapper);

        return count > 0L;
    }

    @Override
    public List<UserVo> getUserVoByConversationId(long conversationId) {
        return conversationUserMapper.getUserVoByConversationId(conversationId);
    }

    @Override
    public boolean getMemberPermission(long uid, long conversationId) {
        return conversationMapper.getMemberPermission(uid, conversationId) > 0;
    }

    @Override
    public boolean removeUserFromConversation(long conversationId, long userIdToRemove) {
        return conversationUserMapper.removeUserFromConversation(conversationId, userIdToRemove) > 0;
    }

    @Override
    public boolean updateMemberPermission(long conversationId, long targetUid, String permission, boolean value) {
        int result;
        ConversationUser conversationUser = conversationUserMapper.selectOne(
                new QueryWrapper<ConversationUser>()
                        .eq("conversation_id", conversationId)
                        .eq("uid", targetUid)
                        .eq("deleted", 0)
        );
        if (conversationUser == null) {
            return false; // 用户不在会话中
        }

        switch (permission) {
            case "memberPermission":
                conversationUser.setMemberPermission(value ? 1 : 0);
                result = conversationUserMapper.updateById(conversationUser);
                break;
            case "fileVisiblePermission":
                conversationUser.setFileVisiblePermission(value ? 1 : 0);
                result = conversationUserMapper.updateById(conversationUser);
                break;
            case "fileOperatePermission":
                conversationUser.setFileOperatePermission(value ? 1 : 0);
                result = conversationUserMapper.updateById(conversationUser);
                break;
            case "messagePermission":
                conversationUser.setMessagePermission(value ? 1 : 0);
                result = conversationUserMapper.updateById(conversationUser);
                break;
            default:
                return false; // 无效的权限类型
        }
        return result > 0;
    }

}
