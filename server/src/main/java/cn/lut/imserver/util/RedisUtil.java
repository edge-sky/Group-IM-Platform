package cn.lut.imserver.util;

import cn.lut.imserver.entity.File;
import cn.lut.imserver.entity.vo.MessageVo;
import cn.lut.imserver.service.ConversationService;
import cn.lut.imserver.service.FileService;
import cn.lut.imserver.service.MessageService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import cn.lut.imserver.entity.Conversation;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Component
@Slf4j
public class RedisUtil {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RedisTemplate<String, String> stringRedisTemplate;
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private MessageService messageService;
    @Autowired
    private FileService fileService;

    public void setUserToken(String uid, String token, long expireTime) {
        stringRedisTemplate.opsForValue().set("User:" + uid + ":token", token, expireTime, TimeUnit.MILLISECONDS);
    }

    public Object get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public boolean checkTokenValid(String uid, String token) {
        String key = "User:" + uid + ":token";
        if (!stringRedisTemplate.hasKey(key)) {
            return false; // 用户不存在或已过期
        }
        String storedToken = stringRedisTemplate.opsForValue().get(key);
        return Objects.equals(storedToken, token);
    }

    // 检查用户是否在会话中
    public boolean isUserInConversation(long conversationId, long uid) {
        String key = "Conversation:" + conversationId + "withUid:" + uid;
        long expireTime = 60 * 60 * 24;

        if (stringRedisTemplate.hasKey(key)) {
            // value 为 "1" 表示存在该会话-用户关系
            return Objects.equals(stringRedisTemplate.opsForValue().get(key), "1");
        }

        // 如果 Redis 中没有记录，则查询数据库
        boolean exists = conversationService.existsByConversationIdAndUid(conversationId, uid) == 1;
        if (exists) {
            // 如果用户在会话中，将结果存入 Redis
            stringRedisTemplate.opsForValue().set(key, "1", expireTime, TimeUnit.SECONDS);
            return true;
        } else {
            // 如果用户不在会话中，将结果存入 Redis
            stringRedisTemplate.opsForValue().set(key, "0", expireTime, TimeUnit.SECONDS);
            return false;
        }
    }

    public long getIncrMessageId(long conversationId) {
        String idKey = "Conversation:" + conversationId + ":msgId";
        String infoKey = "Conversation:" + conversationId + ":info";
        long expireTime = 60 * 60 * 24; // 设置过期时间为 24 小时

        Conversation conversation = conversationService.getById(conversationId);
        if (conversation == null) {
            throw new RuntimeException("会话不存在: " + conversationId);
        }

        // 检查会话信息是否存在于 Redis 中，若存在则刷新过期时间
        if (!stringRedisTemplate.hasKey(idKey)) {
            stringRedisTemplate.opsForValue().set(idKey, String.valueOf(conversation.getLastMessageId()));
        }
        if (!redisTemplate.hasKey(infoKey)) {
            Map<String, Object> fields = new HashMap<>();
            fields.put("name", conversation.getName());
            fields.put("userNum", conversation.getUserNum());
            fields.put("lastMessageId", conversation.getLastMessageId());

            // 将会话信息存入 Redis
            redisTemplate.opsForHash().putAll(infoKey, fields);
            redisTemplate.expire(infoKey, expireTime, TimeUnit.SECONDS);
        } else {
            redisTemplate.expire(infoKey, expireTime, TimeUnit.SECONDS);
        }

        // 自增消息 ID
        Long nextId = stringRedisTemplate.opsForValue().increment(idKey);
        stringRedisTemplate.expire(idKey, expireTime, TimeUnit.SECONDS); // 自增后过期时间需要重新设置
        if (nextId == null) {
            throw new RuntimeException("获取消息 ID 失败");
        }
        return nextId;
    }

    public long getIncrFileVersionId(Long fileId) {
        String idKey = "File:" + fileId + ":versionId";
        long expireTime = 60 * 60 * 24; // 设置过期时间为 24 小时

        File file = fileService.getById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在: " + fileId);
        }

        // 检查文件版本 ID 是否存在于 Redis 中，若不存在则初始化
        if (!stringRedisTemplate.hasKey(idKey)) {
            stringRedisTemplate.opsForValue().set(idKey, String.valueOf(file.getLatestVersion()));
        }

        Long lastestVersion = stringRedisTemplate.opsForValue().increment(idKey);
        stringRedisTemplate.expire(idKey, expireTime, TimeUnit.SECONDS);
        if (lastestVersion == null) {
            throw new RuntimeException("获取文件版本 ID 失败");
        }

        return lastestVersion;
    }

    /**
     * 添加新消息到缓存
     * @param messageVo 消息对象
     * @param expireTime 缓存过期时间，单位为天
     */
    public void addMessageToConversationCache(MessageVo messageVo, long expireTime) {
        if (messageVo == null) {
            log.warn("When adding message to cache, message or message ID is null");
            return;
        }


        String key = "ConvMsg: " + messageVo.getConversationId() + ":messages";
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(messageVo));

        stringRedisTemplate.opsForZSet().add(key, jsonObject.toJSONString(), messageVo.getMessageId());
        stringRedisTemplate.expire(key, expireTime, TimeUnit.DAYS);

        // TODO: 会话 lastMessageID 更新
    }

    /**
     * 拉取 message
     * @param conversationId 会话 ID
     * @param earliestMessageId 最早消息 ID, 向前拉取信息, 若为 0 则表示获取最新消息
     * @param limit 每次获取的消息数量限制
     * @return 返回消息列表
     */
    public List<MessageVo> getMessagesFromConversationCache(long conversationId, long earliestMessageId, int limit) {
        String key = "ConvMsg: " + conversationId + ":messages";
        Set<String> messageSet;

        try {
            // 缓存中无该记录，返回数据库内容且加入缓存
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(key))) {
                List<MessageVo> messagesRes = messageService.getMessageVoWithLimit(conversationId, earliestMessageId, limit);
                messagesRes.forEach(message -> addMessageToConversationCache(message, 7));
                return messagesRes;
            }

            if (earliestMessageId < 0) {
                // 异常参数拒绝
                log.warn("Earliest message ID is invalid: {}", earliestMessageId);
                return Collections.emptyList();
            }

            if (earliestMessageId == 0) {
                messageSet = stringRedisTemplate.opsForZSet().reverseRange(key, 0, limit - 1);
            } else {
                messageSet = stringRedisTemplate.opsForZSet().reverseRangeByScore(key, Double.NEGATIVE_INFINITY, (double) earliestMessageId - 1, 0, limit);
            }

            assert messageSet != null;
            if (messageSet.isEmpty()) {
                // 这段时间的消息不存在缓存中，从数据中拉取
                List<MessageVo> messagesRes = messageService.getMessageVoWithLimit(conversationId, earliestMessageId, limit);
                if (messagesRes.isEmpty()) {
                    return Collections.emptyList();
                }
            }

            if (messageSet.isEmpty()) {
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.warn("When operate redis zset catch exception: {}", String.valueOf(e));
            return Collections.emptyList();
        }

        List<MessageVo> messageVos = new ArrayList<>();
        for (String message : messageSet) {
            messageVos.add(JSON.parseObject(message, MessageVo.class));
        }

        return messageVos;
    }

    public Set<Long> getConvUidList(long conversationId) {
        String key = "Conversation:" + conversationId + ":uidList";
        Set<Object> uidSet = redisTemplate.opsForSet().members(key);
        if (uidSet == null || uidSet.isEmpty()) {
            uidSet = updateConvUidList(conversationId);
        }

        Set<Long> uids = new HashSet<>();
        if (uidSet == null || uidSet.isEmpty()) {
            return uids;
        }
        for (Object uid : uidSet) {
            uids.add((Long) uid);
        }
        return uids;
    }

    public Set<Object> updateConvUidList(long conversationId) {
        String key = "Conversation:" + conversationId + ":uidList";
        if (redisTemplate.hasKey(key)) {
            redisTemplate.delete(key); // 删除旧的缓存
        }

        List<Long> uidList = conversationService.getUserIdsByConversationId(conversationId);
        redisTemplate.opsForSet().add(key, uidList.toArray());
        redisTemplate.expire(key, 60 * 60 * 24, TimeUnit.SECONDS); // 设置过期时间为 24 小时

        return redisTemplate.opsForSet().members(key);
    }

    public void setUnreadNotification(List<Long> uidList, Object notification) {
        if (uidList == null || uidList.isEmpty()) {
            return; // 如果用户列表为空，则不进行任何操作
        }

        for (Long uid : uidList) {
            String key = "User:" + uid + ":unreadNotification";
            redisTemplate.opsForSet().add(key, JSON.toJSONString(notification));
            redisTemplate.expire(key, 7, TimeUnit.DAYS);
        }
    }

    public Set<Object> getUnreadNotification(long uid) {
        String key = "User:" + uid + ":unreadNotification";
        Set<Object> notifications = redisTemplate.opsForSet().members(key);
        redisTemplate.delete(key);
        if (notifications == null || notifications.isEmpty()) {
            return Collections.emptySet();
        }
        return notifications;
    }

    // 设置会话邀请令牌
    // 使用 String 避免长整形精度问题
    public boolean setConversationInviteToken(String token, String conversationId, String inviterFrom, String invitedUid) {
        String invitationKey = "Conversation:" + invitedUid + ":pendingInvitations";

        JSONObject invitation = new JSONObject();
        invitation.put("conversationId", conversationId);
        invitation.put("inviterFrom", inviterFrom);
        invitation.put("token", token);
        invitation.put("timestamp", System.currentTimeMillis());

        stringRedisTemplate.opsForList().leftPush(invitationKey, invitation.toJSONString());
        stringRedisTemplate.expire(invitationKey, 7, TimeUnit.DAYS);

        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(invitationKey));
    }

    // 拉取邀请列表
    public List<JSONObject> getInviteListByUid(long uid) {
        String invitationKey = "Conversation:" + uid + ":pendingInvitations";
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(invitationKey))) {
            return new ArrayList<>(); // 如果没有待处理的邀请，则返回空 JSON 对象
        }
        List<String> invitations = stringRedisTemplate.opsForList().range(invitationKey, 0, -1);
        if (invitations == null || invitations.isEmpty()) {
            return new ArrayList<>(); // 如果没有待处理的邀请，则返回空 JSON 对象
        }

        List<JSONObject> result = new ArrayList<>();
        for (String invitation : invitations) {
            JSONObject tokenInfo = JSON.parseObject(invitation);
            result.add(tokenInfo);
        }
        return result;
    }

    // 验证 token
    public boolean isInviteTokenValid(JSONObject tokenInfo, long invitedUid) {
        String invitationKey = "Conversation:" + invitedUid + ":pendingInvitations";
        if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(invitationKey))) {
            return false; // 邀请令牌不存在或已过期
        }

        boolean success = false;
        List<String> tokenList = stringRedisTemplate.opsForList().range(invitationKey, 0, -1);
        if (tokenList == null) return false;
        List<String> newTokenList = new ArrayList<>();
        for (String token : tokenList) {
            JSONObject currToken = JSON.parseObject(token);
            if (currToken.getLongValue("conversationId") == tokenInfo.getLongValue("conversationId")
                    && currToken.getLongValue("inviterFrom") == tokenInfo.getLongValue("inviterFrom")
                    && currToken.getString("token").equals(tokenInfo.getString("token"))) {
                // 确保有一个token命中
                success = true;
            } else if (currToken.getLongValue("conversationId") != tokenInfo.getLongValue("conversationId")) {
                // 仅保留其他会话目标的邀请
                newTokenList.add(token);
            }
        }
        if (success) {
            // 更新邀请列表
            stringRedisTemplate.delete(invitationKey);
            if (!newTokenList.isEmpty()) {
                stringRedisTemplate.opsForList().rightPushAll(invitationKey, newTokenList);
                stringRedisTemplate.expire(invitationKey, 7, TimeUnit.DAYS);
            }
            return true;
        }
        return false;
    }

    public Set<Object> getPendingInvitations(long uid) {
        String invitationKey = "User:" + uid + ":pendingInvitations";
        if (!redisTemplate.hasKey(invitationKey)) {
            return Collections.emptySet(); // 如果没有待处理的邀请，则返回空集合
        }
        Set<Object> invitations = redisTemplate.opsForSet().members(invitationKey);
        if (invitations == null || invitations.isEmpty()) {
            return Collections.emptySet();
        }
        return invitations;
    }

    /**
     * 从拉取 messages
     * @param conversationId 会话 ID
     * @param start 开始消息 id
     * @param end 结束消息 id
     * @return 返回会话信息
     */

}