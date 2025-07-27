package cn.lut.imserver.controller;

import cn.lut.imserver.entity.Conversation;
import cn.lut.imserver.entity.Folder;
import cn.lut.imserver.entity.User;
import cn.lut.imserver.entity.vo.ConversationVo;
import cn.lut.imserver.entity.vo.UserVo;
import cn.lut.imserver.handle.WebSocketHandle;
import cn.lut.imserver.service.ConversationService;
import cn.lut.imserver.service.FolderService;
import cn.lut.imserver.service.UserService;
import cn.lut.imserver.util.RedisUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/{conversationId}/conversation")
@Slf4j
public class ConversationController {
    @Autowired
    private ConversationService conversationService;

    @Autowired
    private FolderService folderService;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private WebSocketHandle webSocketHandle;

    @PostMapping("/create")
    public ResponseEntity<JSONObject> createConversation(@RequestBody JSONObject jsonObject, HttpServletRequest request, @PathVariable String conversationId) {
        long uid = Long.parseLong((String) request.getAttribute("uid"));

        if (Long.parseLong(conversationId) != 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }

        String name = jsonObject.getString("name");

        if (name == null || name.trim().isEmpty()) {
            JSONObject response = new JSONObject();
            response.put("message", "名字不能为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Conversation conversation = new Conversation();
        conversation.setName(name);
        conversation.setManagerUid(uid);
        conversation.setUserNum(1);
        conversation.setLastMessageId(0); // 初始时没有消息

        boolean created = conversationService.createConversation(conversation);
        if (created) {
            // 初始化根文件夹
            Folder initialFolder = new Folder();
            initialFolder.setName("根文件夹");
            initialFolder.setUpdateUid(uid);
            initialFolder.setPreFolderId(0L);
            initialFolder.setConversationId(conversation.getId());
            folderService.save(initialFolder);

            JSONObject response = new JSONObject();
            response.put("code", "200");
            response.put("message", "创建成功");
            return ResponseEntity.ok().body(response);
        } else {
            JSONObject response = new JSONObject();
            response.put("code", "500");
            response.put("message", "创建失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<ConversationVo>> getConversationsByUserId(HttpServletRequest request, @PathVariable String conversationId) {
        if (Long.parseLong(conversationId) != 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }

        long uid = Long.parseLong((String) request.getAttribute("uid"));
        List<ConversationVo> conversations = conversationService.getConversationsByUserId(uid);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/userInfo")
    public ResponseEntity<List<UserVo>> getUserInfoByConversationId(@PathVariable("conversationId") long conversationId) {
        List<UserVo> userIds = conversationService.getUserVoByConversationId(conversationId);

        return ResponseEntity.ok(userIds);
    }

    @GetMapping("/permission")
    public ResponseEntity<JSONObject> getMemberPermission(@PathVariable("conversationId") long conversationId,
                                                          HttpServletRequest request) {
        long uid = Long.parseLong((String) request.getAttribute("uid"));
        JSONObject response = new JSONObject();

        // 检查用户是否在会话中
        boolean isUserInConversation = conversationService.isUserInConversation(uid, conversationId);
        if (!isUserInConversation) {
            log.debug("用户 {} 不在会话 {} 中，无法获取权限", uid, conversationId);
            
            response.put("code", "403");
            response.put("message", "用户不在会话中");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // 获取用户在会话中的权限
        boolean permission = conversationService.getMemberPermission(uid, conversationId);
        response.put("code", "200");
        response.put("permission", permission);
        return ResponseEntity.ok(response);
    }

    // 邀请用户加入会话
    @PostMapping("/invite")
    public ResponseEntity<JSONObject> invite(@PathVariable("conversationId") long conversationId,
                                             @RequestBody JSONObject payload,
                                             HttpServletRequest request) {
        long uid = Long.parseLong((String) request.getAttribute("uid"));
        String username = (String) request.getAttribute("username");
        long invitedUid = payload.getLongValue("invitedUid");
        JSONObject response = new JSONObject();

        // 判断邀请的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("uid", invitedUid);
        boolean isUserExist = userService.exists(queryWrapper);
        if (!isUserExist) {
            log.debug("用户 {} 不存在，无法发送邀请", invitedUid);

            response.put("code", "400");
            response.put("message", "用户不存在");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 判断用户是否在会话中
        boolean isUserInConversation = conversationService.isUserInConversation(invitedUid, conversationId);
        if (isUserInConversation) {
            log.debug("用户 " + invitedUid + " 已在会话 " + conversationId + " 中，无法发送邀请");

            response.put("code", "400");
            response.put("message", "用户已在会话中");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        boolean success = redisUtil.setConversationInviteToken(token, String.valueOf(conversationId), String.valueOf(uid), String.valueOf(invitedUid));

        if (!success) {
            response.put("code", "500");
            response.put("message", "邀请发送失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("respond", "info");
            jsonObject.put("message", username + " 邀请你加入会话 " + conversationId);
            jsonObject.put("payload", token);
            webSocketHandle.pushJsonObjectToClient(invitedUid, jsonObject);

            response.put("code", "200");
            response.put("message", "邀请发送成功成功");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.debug("When pushing invite message to user {}: {}", invitedUid, e.getMessage());

            response.put("code", "500");
            response.put("message", "邀请发送失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

    }


    @PostMapping("/removeUser")
    public ResponseEntity<JSONObject> removeUserFromConversation(@PathVariable("conversationId") long conversationId,
                                                                 @RequestBody JSONObject jsonObject,
                                                                 HttpServletRequest request) {
        long uid = Long.parseLong((String) request.getAttribute("uid"));
        long userIdToRemove = jsonObject.getLongValue("removeUid");

        JSONObject response = new JSONObject();

//        // 检查用户是否有权限操作
//        boolean hasPermission = conversationService.isOperatePermission(uid, conversationId, true, false, false);
//        if (!hasPermission) {
//            log.debug("用户 " + uid + " 无权操作会话 " + conversationId);
//
//            response.put("code", "403");
//            response.put("message", "无权操作");
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
//        }

        // 检查要移除的用户是否在会话中
        boolean isUserInConversation = conversationService.isUserInConversation(userIdToRemove, conversationId);
        if (!isUserInConversation) {
            log.debug("用户 {} 不在会话 {} 中", userIdToRemove, conversationId);

            response.put("code", "400");
            response.put("message", "用户不在会话中");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 执行移除操作
        boolean removed = conversationService.removeUserFromConversation(conversationId, userIdToRemove);
        if (removed) {
            redisUtil.updateConvUidList(conversationId);
            response.put("code", "200");
            response.put("message", "用户已被移除");
            return ResponseEntity.ok(response);
        } else {
            log.debug("移除用户 {} 失败，可能是会话不存在或其他错误", userIdToRemove);

            response.put("code", "500");
            response.put("message", "移除用户失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/updatePermission")
    public ResponseEntity<JSONObject> updatePermission(@PathVariable("conversationId") long conversationId,
                                                       @RequestBody JSONObject jsonObject,
                                                       HttpServletRequest request) {
        long uid = Long.parseLong((String) request.getAttribute("uid"));
        long targetUid = jsonObject.getLongValue("targetUid");
        String permission = jsonObject.getString("permission");
        boolean value = jsonObject.getBooleanValue("value");
        JSONObject response = new JSONObject();

        // 检查用户是否有权限操作
        boolean hasPermission = conversationService.isOperatePermission(uid, conversationId, true, false, false, false);

        if (conversationService.getById(conversationId).getManagerUid() == targetUid) {
            hasPermission = false;
        }

        // 检查要更新权限的用户是否在会话中
        boolean isUserInConversation = conversationService.isUserInConversation(targetUid, conversationId);
        if (!isUserInConversation) {
            log.debug("用户 {} 不在会话 {} 中", targetUid, conversationId);;

            response.put("code", "400");
            response.put("message", "用户不在会话中");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 更新权限
        boolean updated = conversationService.updateMemberPermission(conversationId, targetUid, permission, value);
        if (updated) {
            redisUtil.updateConvUidList(conversationId);
            response.put("code", "200");
            response.put("message", "权限已更新");
            return ResponseEntity.ok(response);
        } else {
            log.debug("更新用户 {} 权限失败，可能是会话不存在或其他错误", targetUid);

            response.put("code", "500");
            response.put("message", "更新权限失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<String> deleteConversation(@RequestBody Long conversationId) {
        // TODO: 删除会话
        return ResponseEntity.status(500).body("会话删除失败");
    }
}
