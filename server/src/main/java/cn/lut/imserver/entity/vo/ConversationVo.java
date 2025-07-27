package cn.lut.imserver.entity.vo;

import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class ConversationVo {
    private String id; // 会话ID
    private String managerUid; // 会话管理员ID
    private String name; // 会话名称
    private int userNum; // 会话成员数量
    private String lastMessageId; // 会话中最后一条消息id
    private String lastMessageContent; // 会话中最后一条消息内容
    private Date lastMessageTime; // 会话中最后一条消息时间
}
