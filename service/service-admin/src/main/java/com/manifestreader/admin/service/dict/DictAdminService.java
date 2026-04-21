package com.manifestreader.admin.service.dict;

import com.manifestreader.model.entity.DictItemEntity;
import java.util.List;

public interface DictAdminService {

    List<DictItemEntity> listItems(String dictType);
}
