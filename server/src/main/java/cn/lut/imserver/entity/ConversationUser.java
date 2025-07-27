package cn.lut.imserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

@Data
public class ConversationUser {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 关系 id
    private long conversationId; // 会话ID
    private long uid; // 接收用户 ID
    private int memberPermission; // 成员管理权限
    // 操作权限为1的前提是可见权限为1
    private int fileVisiblePermission; // 文件可见权限
    private int fileOperatePermission; // 文件操作权限
    private int messagePermission; // 发言权限
    @TableLogic(value = "0", delval = "1")
    private int deleted; // 逻辑删除标志，0表示未删除，1表示已删除
}
