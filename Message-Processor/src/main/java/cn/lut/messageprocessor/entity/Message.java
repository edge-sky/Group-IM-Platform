package cn.lut.messageprocessor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
public class Message {
    private Long id; // 消息ID
    private String content; // 消息内容
    private int type; //  消息类型
    private long conversationId; // 所属会话
    private long messageId; // 会话中相对 id
    private long fromUid; // 发送者用户ID
    private Date time; // 发送时间
    @TableLogic(value = "0", delval = "1")
    private int withdrawn; // 逻辑撤回标志
    private int isRead; // 是否已读，0表示未读，1表示已读
}