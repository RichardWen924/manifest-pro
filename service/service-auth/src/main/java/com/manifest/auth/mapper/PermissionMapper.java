package com.manifest.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.manifestreader.model.auth.entity.PermissionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends BaseMapper<PermissionEntity> {
}
