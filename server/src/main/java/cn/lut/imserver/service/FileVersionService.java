package cn.lut.imserver.service;

import cn.lut.imserver.entity.FileVersion;
import cn.lut.imserver.entity.vo.FileVersionVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface FileVersionService extends IService<FileVersion> {
    List<FileVersionVo> getFileVersionsByFileId(long fileId);
}
