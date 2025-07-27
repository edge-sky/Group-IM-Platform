package cn.lut.imserver.service;


import cn.lut.imserver.entity.User;
import cn.lut.imserver.entity.vo.UserVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author mingpu
 * @description 针对表【user】的数据库操作Service
 * @createDate 2025-05-06 16:46:21
 */
public interface UserService extends IService<User> {
    boolean register(String username, String password);

    UserVo login(String username, String password);

    UserVo getInfo(long uid);
}
