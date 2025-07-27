package cn.lut.imserver.service;

import cn.lut.imserver.entity.File;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileService extends IService<File> {

    File uploadFile(MultipartFile file, Long folderId, Long uploaderId) throws IOException;

    List<File> getFilesByFolderId(Long folderId);

    boolean deleteFile(Long fileId);

    boolean updateLastestVersion(File targetFile);

    File findByNameAndFolderId(String fileName, long folderId);
}