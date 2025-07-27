package cn.lut.imserver.controller;

import cn.lut.imserver.entity.File;
import cn.lut.imserver.entity.FileVersion;
import cn.lut.imserver.entity.Folder;
import cn.lut.imserver.entity.vo.FileVersionVo;
import cn.lut.imserver.service.FileService;
import cn.lut.imserver.service.FileVersionService;
import cn.lut.imserver.service.FolderService;
import cn.lut.imserver.util.OBSUtil;
import cn.lut.imserver.util.RedisUtil;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@Slf4j
@RequestMapping("/{conversationId}/file")
public class FileController {
    @Autowired
    private OBSUtil obsUtil;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private FileService fileService;
    @Autowired
    private FileVersionService fileVersionService;
    @Autowired
    private FolderService folderService;
    
    @PostMapping("/upload")
    public ResponseEntity<JSONObject> uploadNewFile(@PathVariable("conversationId") long conversationId,
                                                    @RequestParam("file") MultipartFile uploadedFile,
                                                    @RequestParam("fileName") String fileName,
                                                    @RequestParam("preFolderId") long preFolderId,
                                                    @RequestParam("comment") String comment,
                                                    HttpServletRequest request){
        long uid = Long.parseLong((String) request.getAttribute("uid"));
        JSONObject response = new JSONObject();

        if (preFolderId != 0 && !folderService.isFolderInConversation(preFolderId, conversationId)) {
            log.debug("文件夹 {} 不属于会话 {}", preFolderId, conversationId);

            response.put("code", "403");
            response.put("message", "文件夹不存在");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // 如果 preFolderId 为 0，则查询根文件夹
        if (preFolderId == 0) {
            QueryWrapper<Folder> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("conversation_id", conversationId);
            queryWrapper.eq("pre_folder_id", 0);
            preFolderId = folderService.getOne(queryWrapper).getId();
        }

        // 检查上传的文件是否为空
        if (uploadedFile.isEmpty()) {
            log.debug("{} 文件上传失败: 文件内容为空", fileName);

            response.put("code", "400");
            response.put("message", "文件为空");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 1. 通过文件名和文件夹ID从数据库查询File实体
        if (fileName == null || fileName.isEmpty()) {
            // 如果fileName为空，则使用上传的文件名
            fileName = uploadedFile.getOriginalFilename();
        }
        File targetFile = fileService.findByNameAndFolderId(fileName, preFolderId);
        if (targetFile == null) {
            log.debug("文件 {} 在文件夹 {} 中不存在，准备创建新文件记录", fileName, preFolderId);

            // 2. File实体不存在，创建新的File记录
            targetFile = new File();
            targetFile.setName(fileName);
            targetFile.setFolderId(preFolderId);
            targetFile.setLatestVersion(0);
            targetFile.setUpdateTime(new Date());
            targetFile.setCreateTime(new Date());

            try {
                fileService.save(targetFile);
            } catch (Exception e) {
                // 违反唯一约束，可能是并发冲突或其他线程已创建该文件
                // 重新查询以获取最新的File实体
                log.debug("创建新文件记录时可能遇到完整性约束冲突");
                targetFile = fileService.findByNameAndFolderId(fileName, preFolderId);
                if (targetFile == null) {
                    log.debug("无法找到相应文件记录: " + e.getMessage());

                    response.put("code", "500");
                    response.put("message", "文件记录创建失败，请稍后重试");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }
        }

        // 3. 创建FileVersion实例
        FileVersion fileVersion = new FileVersion();
        fileVersion.setFileId(targetFile.getId());
        fileVersion.setComment(comment);

        // 4. 处理文件上传至OBS
        String obsFileName;
        String fileUrl;
        try {
            String originalFilename = uploadedFile.getOriginalFilename();
            String fileSuffix = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileSuffix = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 确保OBS文件名的唯一性
            do {
                obsFileName = UUID.randomUUID() + fileSuffix;
            } while (obsUtil.isDuplicateFileName(obsFileName));
            // 上传文件到OBS
            fileUrl = obsUtil.uploadFile(uploadedFile, obsFileName);
        } catch (Exception e) {
            log.debug("文件上传出现错误: " + e.getMessage());

            response.put("code", "500");
            response.put("message", "文件上传失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // 更新文件版本信息
        fileVersion.setFileUrl(fileUrl);
        long newVersionNumber = redisUtil.getIncrFileVersionId(targetFile.getId());
        // 检查版本号是否超出File实体中latestVersion字段的范围
        if (newVersionNumber > Integer.MAX_VALUE) {
            log.warn("生成的版本号{} 超出Integer最大值，文件ID: {}", newVersionNumber, targetFile.getId());

            response.put("code", "403");
            response.put("message", "版本号超出系统限制");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        fileVersion.setVersion((int) newVersionNumber);
        fileVersion.setUpdateUid(uid);
        fileVersion.setCreateTime(new Date());

        // 5. 保存FileVersion记录
        fileVersionService.save(fileVersion);

        // 6. 更新File实体的 latestVersion 和 updateTime
        targetFile.setLatestVersion((int) newVersionNumber);
        targetFile.setUpdateTime(new Date());
        fileService.updateLastestVersion(targetFile);

        response.put("code", "200");
        response.put("message", "文件上传成功");
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/upload/getUrl")
    public ResponseEntity<JSONObject> getUploadUrl(@PathVariable("conversationId") long conversationId,
                                                   @RequestBody JSONObject requestBody,
                                                   HttpServletRequest request) throws IOException {
        String fileName = requestBody.getString("fileName");
        long preFolderId = requestBody.getLongValue("preFolderId");
        JSONObject response = new JSONObject();

        if (preFolderId != 0 && !folderService.isFolderInConversation(preFolderId, conversationId)) {
            log.debug("文件夹 {} 不属于会话 {}", preFolderId, conversationId);
            response.put("code", "403");
            response.put("message", "文件夹不存在");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        String fileSuffix = "";
        if (fileName != null && fileName.contains(".")) {
            fileSuffix = fileName.substring(fileName.lastIndexOf("."));
        }

        // 生成唯一的对象名
        String objectName;
        do {
            objectName = UUID.randomUUID() + fileSuffix;
        } while (obsUtil.isDuplicateFileName(objectName));

        String temporaryUrl = obsUtil.getUploadUrl(objectName);

        response.put("code", "200");
        response.put("url", temporaryUrl);
        response.put("objectName", objectName); // 返回对象名用于构造文件URL
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/upload/confirm")
    public ResponseEntity<JSONObject> confirmUpload(@PathVariable("conversationId") long conversationId,
                                                    @RequestBody JSONObject requestBody,
                                                    HttpServletRequest request) {

        JSONObject response = new JSONObject();
        long uid = Long.parseLong((String) request.getAttribute("uid"));
        String fileName = requestBody.getString("fileName");
        String fileUrl = requestBody.getString("fileUrl");
        long preFolderId = requestBody.getLongValue("preFolderId");
        String comment = requestBody.getString("comment");

        File targetFile = fileService.findByNameAndFolderId(fileName, preFolderId);
        if (targetFile == null) {
            log.debug("文件 {} 在文件夹 {} 中不存在，准备创建新文件记录", fileName, preFolderId);

            // 2. File实体不存在，创建新的File记录
            targetFile = new File();
            targetFile.setName(fileName);
            targetFile.setFolderId(preFolderId);
            targetFile.setLatestVersion(0);
            targetFile.setUpdateTime(new Date());
            targetFile.setCreateTime(new Date());

            try {
                fileService.save(targetFile);
            } catch (Exception e) {
                // 违反唯一约束，可能是并发冲突或其他线程已创建该文件
                // 重新查询以获取最新的File实体
                log.debug("创建新文件记录时可能遇到完整性约束冲突");
                targetFile = fileService.findByNameAndFolderId(fileName, preFolderId);
                if (targetFile == null) {
                    log.debug("无法找到相应文件记录: " + e.getMessage());

                    response.put("code", "500");
                    response.put("message", "文件记录创建失败，请稍后重试");
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            }
        }
        // 创建FileVersion实例
        FileVersion fileVersion = new FileVersion();
        fileVersion.setFileId(targetFile.getId());
        fileVersion.setComment(comment);

        // 更新文件版本信息
        fileVersion.setFileUrl(fileUrl);
        long newVersionNumber = redisUtil.getIncrFileVersionId(targetFile.getId());
        // 检查版本号是否超出File实体中latestVersion字段的范围
        if (newVersionNumber > Integer.MAX_VALUE) {
            log.warn("生成的版本号{} 超出Integer最大值，文件ID: {}", newVersionNumber, targetFile.getId());

            response.put("code", "403");
            response.put("message", "版本号超出系统限制");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }
        fileVersion.setVersion((int) newVersionNumber);
        fileVersion.setUpdateUid(uid);
        fileVersion.setCreateTime(new Date());

        // 5. 保存FileVersion记录
        fileVersionService.save(fileVersion);

        // 6. 更新File实体的 latestVersion 和 updateTime
        targetFile.setLatestVersion((int) newVersionNumber);
        targetFile.setUpdateTime(new Date());
        fileService.updateLastestVersion(targetFile);

        response.put("code", "200");
        response.put("message", "文件上传确认成功");
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/versionList/{fileId}")
    public ResponseEntity<List<FileVersionVo>> getFileVersionList(@PathVariable("conversationId") long conversationId,
                                                                  @PathVariable("fileId") long fileId) {
        if (fileId <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        List<FileVersionVo> versions = fileVersionService.getFileVersionsByFileId(fileId);
        return ResponseEntity.ok(versions);
    }

    @PostMapping("/move")
    public ResponseEntity<JSONObject> moveFile(@PathVariable("conversationId") long conversationId,
                                               @RequestBody JSONObject requestBody,
                                               HttpServletRequest request) {
        JSONObject response = new JSONObject();
        long fileId = requestBody.getLongValue("fileId");
        long preFolderId = requestBody.getLongValue("preFolderId");

        if (preFolderId != 0 && !folderService.isFolderInConversation(preFolderId, conversationId)) {
            log.debug("文件夹 " + preFolderId + " 不属于会话 " + conversationId);

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

        File file = fileService.getById(fileId);
        if (file == null || file.getFolderId() == preFolderId) {
            log.debug("文件 {} 不存在或已在目标文件夹中", fileId);

            response.put("code", "400");
            response.put("message", "无效的文件或已在目标文件夹中");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        // 更新文件的文件夹ID
        file.setFolderId(preFolderId);
        fileService.updateById(file);

        response.put("code", "200");
        response.put("message", "文件移动成功");
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<JSONObject> removeFile(@PathVariable("conversationId") long conversationId,
                                                 @RequestBody JSONObject requestBody,
                                                 HttpServletRequest request) {
        JSONObject response = new JSONObject();
        long fileId = requestBody.getLongValue("fileId");

        File file = fileService.getById(fileId);
        if (file == null || !folderService.isFolderInConversation(file.getFolderId(), conversationId)) {
            response.put("code", "403");
            response.put("message", "文件不存在或不属于该会话");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // 删除文件记录
        fileService.removeById(fileId);

        response.put("code", "200");
        response.put("message", "文件删除成功");
        return ResponseEntity.ok().body(response);
    }
}

