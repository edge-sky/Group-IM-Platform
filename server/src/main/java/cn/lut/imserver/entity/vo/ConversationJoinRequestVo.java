package cn.lut.imserver.entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class ConversationJoinRequestVo {
    private Long conversationId;
    private List<Long> userIds;
}