package com.manifestreader.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.manifestreader.model.entity.BlDocumentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBillMapper extends BaseMapper<BlDocumentEntity> {
}
