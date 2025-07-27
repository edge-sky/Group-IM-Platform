package cn.lut.imserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class Folder {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id; // 文件夹ID
    private long conversationId;
    private long preFolderId; // 上级文件夹ID，根文件夹为0
    private String name; // 文件夹名称
    private long updateUid;
}
