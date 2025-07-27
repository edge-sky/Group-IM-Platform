package cn.lut.imserver.service.impl;

import cn.lut.imserver.entity.FileVersion;
import cn.lut.imserver.entity.vo.FileVersionVo;
import cn.lut.imserver.mapper.FileVersionMapper;
import cn.lut.imserver.service.FileVersionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileVersionServiceImpl extends ServiceImpl<FileVersionMapper, FileVersion> implements FileVersionService {
    @Autowired
    private FileVersionMapper fileVersionMapper;

    @Override
    public List<FileVersionVo> getFileVersionsByFileId(long fileId) {
        QueryWrapper<FileVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_id", fileId);
        List<FileVersion> fileVersions = fileVersionMapper.selectList(queryWrapper);
        return fileVersions.stream().map(fileVersion -> {
            FileVersionVo fileVersionVo = new FileVersionVo();
            fileVersionVo.setId(String.valueOf(fileVersion.getId()));
            fileVersionVo.setFileId(String.valueOf(fileVersion.getFileId()));
            fileVersionVo.setComment(fileVersion.getComment());
            fileVersionVo.setFileUrl(fileVersion.getFileUrl());
            fileVersionVo.setVersion(fileVersion.getVersion());
            fileVersionVo.setUpdateUid(String.valueOf(fileVersion.getUpdateUid()));
            fileVersionVo.setCreateTime(fileVersion.getCreateTime());
            return fileVersionVo;
        }).toList();
    }
}
