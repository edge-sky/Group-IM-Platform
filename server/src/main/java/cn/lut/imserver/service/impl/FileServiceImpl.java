package cn.lut.imserver.service.impl;

import cn.lut.imserver.entity.File;
import cn.lut.imserver.mapper.FileMapper;
import cn.lut.imserver.service.FileService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    @Autowired
    private FileMapper fileMapper;

    @Override
    public File uploadFile(MultipartFile file, Long folderId, Long uploaderId) throws IOException {
        return null;
    }

    @Override
    public List<File> getFilesByFolderId(Long folderId) {
        return List.of();
    }

    @Override
    public boolean deleteFile(Long fileId) {
        return false;
    }

    @Override
    public boolean updateLastestVersion(File targetFile) {
        return fileMapper.updateLatestVersion(targetFile) > 0;
    }

    @Override
    public File findByNameAndFolderId(String fileName, long folderId) {
        return fileMapper.findByNameAndFolderId(fileName, folderId);
    }
}