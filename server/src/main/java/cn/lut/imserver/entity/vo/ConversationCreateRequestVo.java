package cn.lut.imserver.entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class ConversationCreateRequestVo {
    private String conversationName;
    private List<Long> relatedUserIds;
}