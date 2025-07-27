package cn.lut.imserver.entity.vo;

import lombok.Data;

@Data
public class UserVo {
    private String uid;
    private String username; // 用户名
    private String avatar; // 用户头像
    private int memberPermission; // 人员管理权限
    private int fileVisiblePermission; // 文件可见权限
    private int fileOperatePermission; // 文件操作权限
    private int messagePermission; // 发言权限
    private int deleted;
}