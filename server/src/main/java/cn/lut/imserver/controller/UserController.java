package cn.lut.imserver.controller;

import cn.lut.imserver.entity.User;
import cn.lut.imserver.entity.vo.JoinRequestDto;
import cn.lut.imserver.entity.vo.UserVo;
import cn.lut.imserver.service.ConversationService;
import cn.lut.imserver.service.impl.UserServiceImpl;
import cn.lut.imserver.util.JWTUtil;
import cn.lut.imserver.util.RedisUtil;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@Slf4j
@RequestMapping("/user")
@CrossOrigin
public class UserController {
    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;
    
    // 用户注册
    @PostMapping("/register")
    public ResponseEntity<JSONObject> register(@RequestBody User userInfo) {
        // 参数验证
        if (userInfo.getUsername() == null || userInfo.getUsername().trim().isEmpty()) {
            JSONObject response = new JSONObject();
            response.put("message", "用户名不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        if (userInfo.getPassword() == null || userInfo.getPassword().trim().isEmpty()) {
            JSONObject response = new JSONObject();
            response.put("message", "密码不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        if (userInfo.getUsername().length() < 3 || userInfo.getUsername().length() > 20) {
            JSONObject response = new JSONObject();
            response.put("message", "用户名长度必须在3-20个字符之间");
            return ResponseEntity.badRequest().body(response);
        }
        if (userInfo.getPassword().length() < 6) {
            JSONObject response = new JSONObject();
            response.put("message", "密码长度不能少于6个字符");
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean success = userService.register(userInfo.getUsername(), userInfo.getPassword());
        if (success) {
            JSONObject response = new JSONObject();
            response.put("message", "注册成功");
            return ResponseEntity.ok(response);
        } else {
            JSONObject response = new JSONObject();
            response.put("message", "用户名已存在");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }
    
    /**
     * 用户登录
     * @param userInfo 用户登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    public ResponseEntity<JSONObject> login(@RequestBody User userInfo) {
        // 参数验证
        if (userInfo.getUsername() == null || userInfo.getUsername().trim().isEmpty()) {
            JSONObject response = new JSONObject();
            response.put("respond", "usernameNull");
            return ResponseEntity.badRequest().body(response);
        }
        if (userInfo.getPassword() == null || userInfo.getPassword().trim().isEmpty()) {
            JSONObject response = new JSONObject();
            response.put("respond", "passwordNull");
            return ResponseEntity.badRequest().body(response);
        }
        
        UserVo user = userService.login(userInfo.getUsername(), userInfo.getPassword());
        JSONObject response = new JSONObject();
        if (user != null) {
            String token = jwtUtil.createToken(user.getUid(), userInfo.getUsername());
            response.put("respond", "loginSuccess");
            response.put("token", token);
            response.put("uid", user.getUid());
            return ResponseEntity.ok(response);
        } else {
            response.put("respond", "errorSubmit");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<JSONObject> verifyLogin() {
        JSONObject response = new JSONObject();
        response.put("respond", "tokenValid");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<JSONObject> getUserInfo(HttpServletRequest request) {
        long uid = Long.parseLong((String) request.getAttribute("uid"));
        UserVo userVo = userService.getInfo(uid);

        if (userVo == null) {
            JSONObject response = new JSONObject();
            response.put("code", "404");
            response.put("message", "用户不存在");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        JSONObject response = new JSONObject();
        response.put("code", "200");
        response.put("user", userVo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inviteList")
    public ResponseEntity<JSONObject> getInviteList(HttpServletRequest request) {
        long uid = Long.parseLong((String) request.getAttribute("uid"));
        JSONObject response = new JSONObject();

        // 获取用户的邀请列表
        List<JSONObject> inviteList = redisUtil.getInviteListByUid(uid);
        if (inviteList == null || inviteList.isEmpty()) {
            response.put("code", "200");
            response.put("message", "没有新的邀请");
            return ResponseEntity.ok(response);
        }

        response.put("code", "200");
        response.put("invites", inviteList);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join")
    public ResponseEntity<JSONObject> joinConversation(@RequestBody JoinRequestDto joinRequestDto,
                                                       HttpServletRequest request) {
        long uid = Long.parseLong((String) request.getAttribute("uid"));
        JSONObject response = new JSONObject();

        JSONObject inviteInfo = joinRequestDto.getInviteInfo();
        // 从 inviteInfo 对象中获取 accept 和 conversationId
        boolean accept = inviteInfo.getBooleanValue("accept");
        long conversationId = inviteInfo.getLongValue("conversationId");

        boolean valid = redisUtil.isInviteTokenValid(inviteInfo, uid);
        if (!valid) {
            log.debug("邀请 token 无效或已过期，用户 {} 尝试加入会话 {}", uid, conversationId);

            response.put("code", "400");
            response.put("message", "邀请已失效");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (!accept) {
            log.info("用户 {} 拒绝加入会话 {}", uid, conversationId);

            response.put("code", "200");
            response.put("message", "已拒绝加入会话");
            return ResponseEntity.ok(response);
        }

        boolean joined = conversationService.addUsersToConversation(conversationId, uid);
        if (!joined) {
            log.info("用户 {} 加入会话 {} 失败", uid, conversationId);

            response.put("code", "500");
            response.put("message", "加入会话失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        redisUtil.updateConvUidList(conversationId);
        response.put("code", "200");
        response.put("message", "加入会话成功");
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
