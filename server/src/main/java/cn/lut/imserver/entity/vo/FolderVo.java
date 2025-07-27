package cn.lut.imserver.entity.vo;

import cn.lut.imserver.entity.File;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FolderVo {
    private String id;
    private String name;
    private List<FileVo> files;
    private List<FolderVo> subFolders;

    public FolderVo() {
        this.files = new ArrayList<>();
        this.subFolders = new ArrayList<>();
    }

    public FolderVo(String id, String name) {
        this.id = id;
        this.name = name;
        this.files = new ArrayList<>();
        this.subFolders = new ArrayList<>();
    }
}
