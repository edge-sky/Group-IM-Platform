package cn.lut.imserver.service.impl;

import cn.lut.imserver.entity.User;
import cn.lut.imserver.entity.vo.UserVo;
import cn.lut.imserver.mapper.UserMapper;
import cn.lut.imserver.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import cn.hutool.crypto.digest.DigestUtil;

import java.util.Date;
import java.util.UUID;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @return 注册结果
     */
    public boolean register(String username, String password) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(username);
        if (existingUser != null) {
            return false; // 用户名已存在
        }
        
        // 生成盐值
        String salt = UUID.randomUUID().toString().replace("-", "");
        
        // 加密密码
        String encryptedPassword = DigestUtil.sha256Hex(password + salt);
        
        // 创建用户对象
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptedPassword);
        user.setAvatar("https://shopping.obs.cn-south-1.myhuaweicloud.com/b_81edd3ea1924ffef65801eed165810ab.jpg"); // 设置默认头像
        user.setSalt(salt);
        user.setStatus(0);
        user.setCreateTime(new Date());
        user.setDeleted(0);
        
        // 保存用户
        return userMapper.insert(user) > 0;
    }
    
    /**
     * 用户登录验证
     * @param username 用户名
     * @param password 密码
     * @return 用户信息，登录失败返回null
     */
    public UserVo login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null; // 用户不存在
        }
        
        // 验证密码
        String encryptedPassword = DigestUtil.sha256Hex(password + user.getSalt());
        if (encryptedPassword.equals(user.getPassword())) {
            UserVo userVo = new UserVo();
            userVo.setUid(String.valueOf(user.getUid()));
            return userVo;
        }
        
        return null; // 密码错误
    }

    public UserVo getInfo(long uid) {
        User user = userMapper.selectById(uid);
        if (user == null) {
            return null; // 用户不存在
        }

        UserVo userVo = new UserVo();
        userVo.setUid(String.valueOf(user.getUid()));
        userVo.setUsername(user.getUsername());
        userVo.setAvatar(user.getAvatar());
        return userVo;
    }
}