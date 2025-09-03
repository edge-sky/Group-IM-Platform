package cn.lut.messageprocessor.service;

public interface ConversationService {
    void updateLastMessageId(long conversationId, long lastMessageId);
}