package cn.lut.imserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;

@Data
public class File {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private long folderId;
    private String name;
    private int latestVersion;
    private Date updateTime;
    private Date createTime;
    @TableLogic(value = "0", delval = "1")
    private int deleted; // 逻辑删除标志，0表示未删除，1表示已删除
}
