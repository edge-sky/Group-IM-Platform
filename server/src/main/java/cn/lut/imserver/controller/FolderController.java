package cn.lut.imserver.controller;

import cn.lut.imserver.entity.Folder;
import cn.lut.imserver.entity.vo.FolderCreateVo;
import cn.lut.imserver.entity.vo.FolderVo;
import cn.lut.imserver.service.ConversationService;
import cn.lut.imserver.service.FolderService;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@Slf4j
@RequestMapping("{conversationId}/folder")
public class FolderController {
    @Autowired
    private ConversationService conversationService;
    @Autowired
    private FolderService folderService;

    @RequestMapping("/create")
    public ResponseEntity<JSONObject> createFolder(@PathVariable("conversationId") long conversationId,
                                                   @RequestBody FolderCreateVo folderVo,
                                                   HttpServletRequest request) {
        long uid = Long.parseLong((String) request.getAttribute("uid"));

        if (folderVo.getParentId() == 0) {
            QueryWrapper<Folder> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("conversation_id", conversationId);
            queryWrapper.eq("pre_folder_id", 0);
            folderVo.setParentId(folderService.getOne(queryWrapper).getId());
        }

        Folder folder = new Folder();
        folder.setName(folderVo.getName());
        folder.setUpdateUid(uid);
        folder.setPreFolderId(folderVo.getParentId());
        folder.setConversationId(conversationId);

        try {
            folderService.save(folder);

            JSONObject response = new JSONObject();
            response.put("code", "200");
            response.put("message", "创建成功");
            return ResponseEntity.ok().body(response);
        } catch (DataIntegrityViolationException e) {
            JSONObject response = new JSONObject();
            response.put("code", "409");
            response.put("message", "文件夹已存在");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<FolderVo> listFiles(@PathVariable("conversationId") long conversationId) {
        FolderVo folderVo = folderService.getSubFolderList(conversationId, 0);
        if (folderVo == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        return ResponseEntity.ok(folderVo);
    }

    @PostMapping("/move")
    public ResponseEntity<JSONObject> moveFolder(@PathVariable("conversationId") long conversationId,
                                                 @RequestBody JSONObject requestBody,
                                                 HttpServletRequest request) {
        JSONObject response = new JSONObject();
        long folderId = requestBody.getLongValue("folderId");
        long preFolderId = requestBody.getLongValue("preFolderId");

        if (preFolderId != 0 && !folderService.isFolderInConversation(folderId, conversationId)) {
            log.warn("文件夹 {} 不属于会话 {}", folderId, conversationId);

            response.put("code", "403");
            response.put("message", "目标文件夹不存在");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        if (preFolderId == 0) {
            QueryWrapper<Folder> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("conversation_id", conversationId);
            queryWrapper.eq("pre_folder_id", 0);
            preFolderId = folderService.getOne(queryWrapper).getId();
        }

        Folder folder = folderService.getById(folderId);
        if (folder == null || folder.getPreFolderId() == preFolderId) {
            log.warn("文件 {} 不存在或已在目标文件夹中", folderId);

            response.put("code", "400");
            response.put("message", "无效的文件或已在目标文件夹中");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 更新文件的文件夹ID
        folder.setPreFolderId(preFolderId);
        folderService.updateById(folder);

        response.put("code", "200");
        response.put("message", "文件移动成功");
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<JSONObject> removeFolder(@PathVariable("conversationId") long conversationId,
                                                   @RequestBody JSONObject requestBody,
                                                   HttpServletRequest request) {
        JSONObject response = new JSONObject();
        long folderIdLong = requestBody.getLongValue("folderId");

        if (!folderService.isFolderInConversation(folderIdLong, conversationId)) {
            response.put("code", "403");
            response.put("message", "文件夹不存在或不属于该会话");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        folderService.removeFolder(folderIdLong, conversationId);
        response.put("code", "200");
        response.put("message", "文件夹删除成功");
        return ResponseEntity.ok().body(response);
    }
}


