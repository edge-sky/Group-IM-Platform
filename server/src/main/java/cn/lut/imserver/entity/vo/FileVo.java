package cn.lut.imserver.entity.vo;

import lombok.Data;

import java.util.Date;

@Data
public class FileVo {
    private String id;
    private String folderId;
    private String name;
    private int latestVersion;
    private Date updateTime;
    private Date createTime;
}
