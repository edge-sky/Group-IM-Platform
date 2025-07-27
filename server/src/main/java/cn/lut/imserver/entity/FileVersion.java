package cn.lut.imserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class FileVersion {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private long fileId; // 文件ID
    private String comment; // 版本备注
    private String fileUrl; // 文件存储地址
    private int version; // 版本号
    private long updateUid; // 更新的用户ID
    private Date createTime;
}
