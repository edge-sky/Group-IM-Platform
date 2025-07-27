package cn.lut.imserver.entity.vo;

import lombok.Data;

@Data
public class FolderCreateVo {
    private String name; // 文件夹名称
    private long parentId; // 父文件夹ID，根目录为0
}
