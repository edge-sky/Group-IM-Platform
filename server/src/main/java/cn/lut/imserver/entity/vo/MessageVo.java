package cn.lut.imserver.entity.vo;

import lombok.Data;

import java.util.Date;

@Data
public class MessageVo {
    private String conversationId; // 会话ID
    private long messageId; // 消息ID
    private String content; // 消息内容
    private int type; //  消息类型
    private String fromUid; // 发送者ID
    private Date time;
    private int withdrawn; // 是否撤回
    private int isRead;
}
