package cn.lut.imserver.service.impl;

import cn.lut.imserver.service.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalStorageServiceImpl implements StorageService {

    @Value("${file.upload-dir:./uploads}") // 从配置文件读取上传目录，默认为./uploads
    private String uploadDir;

    @Override
    public String uploadFile(MultipartFile multipartFile) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = multipartFile.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String uniqueFileName = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(uniqueFileName);

        Files.copy(multipartFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString(); // 返回文件的相对路径或绝对路径，取决于您的需求
    }

    @Override
    public boolean deleteFile(String filePathString) throws IOException {
        Path filePath = Paths.get(filePathString);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            return true;
        }
        return false; // 文件不存在
    }
}