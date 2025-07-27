package cn.lut.imserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.util.Date;

@Data
public class User {
    @TableId(value = "uid", type = IdType.ASSIGN_ID)
    private Long uid; // 用户ID
    private String username; // 用户名
    private String password; // 密码
    private String avatar; // 头像URL
    private String salt; // 盐值
    private int status; // 用户状态
    private Date createTime; // 创建时间
    @TableLogic(value = "0", delval = "1")
    private int deleted; // 逻辑删除标志
}
