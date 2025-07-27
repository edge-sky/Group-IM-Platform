package cn.lut.imserver.service.impl;

import cn.lut.imserver.entity.Folder;
import cn.lut.imserver.entity.vo.FileVo;
import cn.lut.imserver.entity.vo.FolderVo;
import cn.lut.imserver.mapper.FileMapper;
import cn.lut.imserver.mapper.FolderMapper;
import cn.lut.imserver.service.FolderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FolderServiceImpl extends ServiceImpl<FolderMapper, Folder> implements FolderService {
    @Autowired
    FolderMapper folderMapper;
    @Autowired
    FileMapper fileMapper;

    @Override
    public boolean isFolderInConversation(long folderId, long conversationId) {
        return folderMapper.countFolderWithConversation(folderId, conversationId) > 0;
    }

    @Override
    public FolderVo getSubFolderList(long conversationId, long folderId) {
        List<Folder> folders = folderMapper.getSubFolderList(conversationId, folderId);
        List<FileVo> files = fileMapper.getFilesByConversation(conversationId);

        Map<Long, FolderVo> folderVoMap = folders.stream()
                .collect(Collectors.toMap(Folder::getId, folder -> new FolderVo(String.valueOf(folder.getId()), folder.getName())));
        for (FileVo file : files) {
            folderVoMap.get(Long.parseLong(file.getFolderId())).getFiles().add(file);
        }

        FolderVo root = null;
        for (Folder folder : folders) {
            // preFolderId为0的是顶级文件夹
            if (folder.getPreFolderId() == 0) {
                root = folderVoMap.get(folder.getId());
            } else {
                // 找到父文件夹，并将当前文件夹Vo添加为其子文件夹
                FolderVo parentVo = folderVoMap.get(folder.getPreFolderId());
                FolderVo childVo = folderVoMap.get(folder.getId());
                if (parentVo != null && childVo != null) {
                    parentVo.getSubFolders().add(childVo);
                }
            }
        }

        return root;
    }

    @Override
    @Transactional
    public boolean removeFolder(long folderId, long conversationId) {
        // 获取文件树
        List<Folder> subFolders = folderMapper.getSubFolderList(conversationId, folderId);
        // 结果不包含当前文件夹
        List<Long> subFolderIds = new java.util.ArrayList<>(subFolders.stream()
                .map(Folder::getId)
                .distinct() // 去重
                .toList());
        subFolderIds.add(folderId); // 添加当前文件夹ID

        // 删除子文件
        fileMapper.removeBatchFilesByFolderIds(subFolderIds);

        // 删除文件夹
        return folderMapper.removeBatchFolders(subFolderIds) > 0;
    }
}