package cn.lut.imserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

@Data
public class Conversation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 会话ID
    private long managerUid; // 会话管理员ID
    private String name; // 会话名称
    private int userNum; // 会话成员数量
    private long lastMessageId; // 会话中最后一条消息id
    @TableLogic(value = "0", delval = "1")
    private int deleted; // 逻辑删除标志，0表示未删除，1表示已删除
}
