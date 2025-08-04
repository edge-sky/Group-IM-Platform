package cn.lut.imserver.handle;

import cn.lut.imserver.entity.Message;
import cn.lut.imserver.entity.vo.MessageVo;
import cn.lut.imserver.service.ConversationService;
import cn.lut.imserver.util.MqUtil;
import cn.lut.imserver.util.RedisUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

@Component
public class WebSocketHandle extends TextWebSocketHandler {
    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private static int onlineCount = 0;
    private static final Set<WebSocketSession> sessionList = new CopyOnWriteArraySet<>();
    private static final Map<Long, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MqUtil mqUtil;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        // 从 session 中获取 uid
        long uid = Long.parseLong((String) session.getAttributes().get("uid"));

        logger.info("WebSocket connection established: " + session.getId() + ", uid: " + uid);
        JSONObject hello = new JSONObject();
        hello.put("respond", "connectionEstablished");
        session.sendMessage(new TextMessage(hello.toJSONString()));

        sessionList.add(session);
        userSessionMap.put(uid, session);
        onlineCount = sessionList.size();

        Set<Object> unRead = redisUtil.getUnreadNotification(uid);
        if (!unRead.isEmpty()) {
            JSONObject response = new JSONObject();
            response.put("respond", "unreadNotification");
            response.put("message", "Unread notifications found");
            response.put("payload", unRead);
            session.sendMessage(new TextMessage(response.toJSONString()));
        }

        Set<Object> inviteList = redisUtil.getPendingInvitations(uid);
        if (!inviteList.isEmpty()) {
            JSONObject response = new JSONObject();
            response.put("respond", "pendingInvitations");
            response.put("message", "Pending invitations found");
            response.put("payload", inviteList);
            session.sendMessage(new TextMessage(response.toJSONString()));
        }
    }

    @Override
    public void handleTextMessage(@NotNull WebSocketSession session, TextMessage message) throws IOException {
        logger.info("Received message: " + message.getPayload());
        JSONObject jsonObject;
        JSONObject response = new JSONObject();

        try {
            jsonObject = JSON.parseObject(message.getPayload());
        } catch (Exception e) {
            logger.warning("When parsing JSON catch error: " + e.getMessage());

            response.put("respond", "error");
            response.put("code", "400");
            response.put("message", "Invalid JSON format");
            session.sendMessage(new TextMessage(response.toJSONString()));
            return;
        }

        // 检查请求类型
        String requestType = jsonObject.getString("request");
        long currentUid = Long.parseLong((String) session.getAttributes().get("uid"));
        long conversationId = jsonObject.getLongValue("conversationId");
        if (requestType == null) {
            logger.warning("Request is null");

            response.put("respond", "error");
            response.put("code", "400");
            response.put("message", "Unknown request");
            session.sendMessage(new TextMessage(response.toJSONString()));
            return;
        }

        // 匹配请求
        if (requestType.equalsIgnoreCase("sendMessage")) {
            if (conversationId <= 0) {
                logger.warning("Conversation ID is invalid: " + conversationId);

                response.put("respond", "error");
                response.put("code", "400");
                response.put("message", "Invalid conversation ID");
                session.sendMessage(new TextMessage(response.toJSONString()));
                return;
            }

            // 检查用户是否有发送消息的权限
            boolean hasPermission = conversationService.isOperatePermission(
                    currentUid,
                    conversationId,
                    false, // 不需要成员操作权限
                    true,  // 需要消息权限
                    false, // 不需要文件可见权限
                    false  // 不需要文件操作权限
            );

            if (!hasPermission) {
                logger.warning("User does not have permission to send messages in conversation ID: " + conversationId);

                response.put("respond", "error");
                response.put("code", "403");
                response.put("message", "Permission denied");
                session.sendMessage(new TextMessage(response.toJSONString()));
                return;
            }

            logger.info("Processing sendMessage request for conversation ID: " + conversationId);
            String content = jsonObject.getString("content");
            int type = jsonObject.getIntValue("type");
            onMessage(currentUid, conversationId, content, type);
        } else if (requestType.equalsIgnoreCase("fetchMessages")) {
            if (conversationId <= 0) {
                logger.warning("Conversation ID is invalid: " + conversationId);

                response.put("respond", "error");
                response.put("code", "400");
                response.put("message", "Invalid conversation ID");
                session.sendMessage(new TextMessage(response.toJSONString()));
                return;
            }
            long earliestMessageId = jsonObject.getLongValue("earliestMessageId");
            int limit = Math.max(jsonObject.getIntValue("limit"), 50);
            List<MessageVo> messageVoList = fetchMessages(conversationId, earliestMessageId, limit);

            if (messageVoList.isEmpty()) {
                logger.info("No messages found for conversation ID: " + conversationId);

                response.put("respond", "messagesList");
                response.put("code", "200");
                response.put("payload", Collections.emptyList());
                response.put("message", "No messages found");
            } else {
                logger.info("Found " + messageVoList.size() + " messages for conversation ID: " + conversationId);

                response.put("respond", "messagesList");
                response.put("code", "200");
                response.put("message", "Messages fetched successfully");
                response.put("payload", messageVoList);
            }
        } else {
            logger.warning("Unknown request type: " + requestType);

            response.put("respond", "error");
            response.put("code", "400");
            response.put("message", "Unknown request type");
            session.sendMessage(new TextMessage(response.toJSONString()));
        }
        pushJsonObjectToClient(currentUid, response);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) {
        long uid = Long.parseLong((String) session.getAttributes().get("uid"));

        boolean removed = sessionList.remove(session);
        userSessionMap.remove(uid, session); // Remove the specific session for the UID
        logger.info("WebSocket connection closed: " + session.getId() + ", uid: " + uid + " with status: " + status);

        if (removed) {
            onlineCount = sessionList.size(); // More accurate count
        }
        logger.info("User disconnected. Total online users: " + onlineCount);
    }

    private void onMessage(long fromUid, long conversationId, String content, int type) throws IOException {
        // 封装 message
        Message msg = new Message();
        msg.setContent(content);
        msg.setType(type);
        msg.setConversationId(conversationId);
        msg.setFromUid(fromUid);
        msg.setTime(new Date());
        msg.setWithdrawn(0);
        msg.setIsRead(0);
        msg.setMessageId(redisUtil.getIncrMessageId(conversationId));

        JSONObject jsonObject = new JSONObject();
        try {
            // 将消息加入消息队列
            String messageJson = JSON.toJSONString(msg);
            mqUtil.sendMessage("save-message", String.valueOf(msg.getId()), messageJson);
        } catch (Exception e) {
            jsonObject.put("respond", "sendMessageCallback");
            jsonObject.put("code", "500");
            jsonObject.put("message", "消息发送失败");
            jsonObject.put("payload", JSON.toJSONString(msg));
            pushJsonObjectToClient(fromUid, jsonObject);
        }
    }

    public void pushJsonObjectToClient(long uid, JSONObject payload) throws IOException {
        WebSocketSession targetSession = userSessionMap.get(uid);
        if (targetSession != null) {
            targetSession.sendMessage(new TextMessage(payload.toJSONString()));
        }
        // todo: 若用户不在线，加入等待列表
        redisUtil.setUnreadNotification(List.of(uid), payload);
    }

    public void pushJsonObjectToMultiClient(Set<Long> uidList, JSONObject payload) throws IOException {
        List<Long> offlineUidList = new ArrayList<>();
        for (long uid : uidList) {
            WebSocketSession targetSession = userSessionMap.get(uid);
            if (targetSession != null) {
                targetSession.sendMessage(new TextMessage(payload.toJSONString()));
            } else {
                // todo: 若用户不在线，加入等待列表
                offlineUidList.add(uid);
            }
        }
        redisUtil.setUnreadNotification(offlineUidList, payload);
    }

    List<MessageVo> fetchMessages(long conversationId, long earliestMessageId, int limit) {
        if (conversationId <= 0) {
            logger.warning("Conversation ID is invalid: " + conversationId);
            return Collections.emptyList();
        }

        List<MessageVo> res = redisUtil.getMessagesFromConversationCache(conversationId, earliestMessageId, limit);

        return res;
    }
}

