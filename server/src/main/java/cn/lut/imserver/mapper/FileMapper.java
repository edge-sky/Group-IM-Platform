package cn.lut.imserver.mapper;

import cn.lut.imserver.entity.File;
import cn.lut.imserver.entity.vo.FileVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMapper extends BaseMapper<File> {
    int updateLatestVersion(File targetFile);

    File findByNameAndFolderId(@Param("name") String fileName, @Param("folderId") long folderId);

    List<FileVo> getFilesByConversation(long conversationId);

    void removeBatchFilesByFolderIds(List<Long> subFolderIds);
}