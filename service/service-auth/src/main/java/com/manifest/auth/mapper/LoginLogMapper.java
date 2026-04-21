package com.manifest.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.manifestreader.model.auth.entity.LoginLogEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLogEntity> {
}
