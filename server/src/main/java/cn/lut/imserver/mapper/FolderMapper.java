package cn.lut.imserver.mapper;

import cn.lut.imserver.entity.Folder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FolderMapper extends BaseMapper<Folder> {
    int countFolderWithConversation(@Param("folderId") long folderId, @Param("conversationId") long conversationId);

    List<Folder> getSubFolderList(long conversationId, long folderId);

    int removeBatchFolders(@Param("subFolderIds") List<Long> subFolderIds);
}