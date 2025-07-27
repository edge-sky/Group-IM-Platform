package cn.lut.imserver.util;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class OBSUtil {
    @Value("${obs.endpoint}")
    private String endpoint;
    @Value("${obs.ak}")
    private String ak;
    @Value("${obs.sk}")
    private String sk;
    @Value("${obs.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String objectName) throws IOException {
        // 使用结束后释放资源
        try (ObsClient obsClient = new ObsClient(ak, sk, endpoint);
             InputStream inputStream = file.getInputStream()) {

            PutObjectResult result = obsClient.putObject(bucketName, objectName, inputStream);
            log.info("文件上传成功: {}", objectName);
            return result.getObjectUrl(); // 返回文件的访问URL
        } catch (ObsException e) {
            log.error("OBS上传失败");
            log.error("HTTP Code: {}", e.getResponseCode());
            log.error("Error Code: {}", e.getErrorCode());
            log.error("Error Message: {}", e.getErrorMessage());
            log.error("Request ID: {}", e.getErrorRequestId());
            log.error("Host ID: {}", e.getErrorHostId());
            log.error("OBS异常详情: ", e);
            throw new ObsException("OBS上传失败");
        } catch (IOException e) {
            log.error("文件读取失败: {}", e.getMessage(), e);
            throw new IOException("文件读取失败: " + e.getMessage());
        }
    }

    public String getUploadUrl(String objectName) {
        try (ObsClient obsClient = new ObsClient(ak, sk, endpoint)) {

            // 生成预签名的上传URL
            Map<String, String> headers = new HashMap<>();
            String contentType = "application/octet-stream";
            headers.put("Content-Type", contentType);
            // URL有效期，3600秒
            long expireSeconds = 3600L;
            TemporarySignatureRequest request = new TemporarySignatureRequest(HttpMethodEnum.PUT, bucketName, objectName, SpecialParamEnum.CORS, expireSeconds);
            request.setHeaders(headers);

            TemporarySignatureResponse signatureResponse = obsClient.createTemporarySignature(request);

            return signatureResponse.getSignedUrl();

        } catch (ObsException | IOException e) {
            log.error("生成上传URL失败: {}", e.getMessage(), e);
            return null;
        }
    }

    // 检查文件名是否重复
    public boolean isDuplicateFileName(String fileName) throws IOException {
        try (ObsClient obsClient = new ObsClient(ak, sk, endpoint)) {
            boolean exists = obsClient.doesObjectExist(bucketName, fileName);
            log.debug("文件存在性检查 - 文件名: {}, 存在: {}", fileName, exists);
            return exists;
        } catch (ObsException e) {
            log.error("检查文件重复性失败");
            log.error("HTTP Code: {}", e.getResponseCode());
            log.error("Error Code: {}", e.getErrorCode());
            log.error("Error Message: {}", e.getErrorMessage());
            log.error("Request ID: {}", e.getErrorRequestId());
            log.error("Host ID: {}", e.getErrorHostId());
            log.error("OBS异常详情: ", e);
            throw new ObsException("OBS检查失败");
        }
    }
}