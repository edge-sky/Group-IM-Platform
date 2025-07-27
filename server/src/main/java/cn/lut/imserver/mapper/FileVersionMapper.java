package cn.lut.imserver.mapper;

import cn.lut.imserver.entity.File;
import cn.lut.imserver.entity.FileVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileVersionMapper extends BaseMapper<FileVersion> {
}
