package cn.lut.imserver.service;

import cn.lut.imserver.entity.Folder;
import cn.lut.imserver.entity.vo.FolderVo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface FolderService extends IService<Folder> {

    boolean isFolderInConversation(long folderId, long conversationId);

    FolderVo getSubFolderList(long conversationId, long folderId);

    boolean removeFolder(long folderId, long conversationId);
}