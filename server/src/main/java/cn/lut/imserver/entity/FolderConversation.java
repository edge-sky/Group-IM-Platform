package cn.lut.imserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class FolderConversation {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private long conversationId; // 会话ID
    private long folderId; // 文件夹ID
}
