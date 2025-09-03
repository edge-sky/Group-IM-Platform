package cn.lut.messageprocessor.service.impl;

import cn.lut.messageprocessor.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ConversationServiceImpl implements ConversationService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void updateLastMessageId(long conversationId, long lastMessageId) {
        String sql = "UPDATE conversation SET last_message_id = ? WHERE id = ?";
        jdbcTemplate.update(sql, lastMessageId, conversationId);
    }
}