package com.manifestreader.admin.service.dict;

import com.manifestreader.model.entity.DictItemEntity;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DictAdminServiceImpl implements DictAdminService {

    @Override
    public List<DictItemEntity> listItems(String dictType) {
        return Collections.emptyList();
    }
}
