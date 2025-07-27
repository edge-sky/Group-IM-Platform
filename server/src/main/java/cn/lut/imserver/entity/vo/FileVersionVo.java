package cn.lut.imserver.entity.vo;

import lombok.Data;

import java.util.Date;

@Data
public class FileVersionVo {
    private String id;
    private String fileId; // 文件ID
    private String comment; // 版本备注
    private String fileUrl; // 文件存储地址
    private int version; // 版本号
    private String updateUid; // 更新的用户ID
    private Date createTime;
}
