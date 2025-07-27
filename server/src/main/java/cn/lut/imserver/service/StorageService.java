package cn.lut.imserver.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface StorageService {

    String uploadFile(MultipartFile file) throws IOException;

    boolean deleteFile(String filePath) throws IOException;

}