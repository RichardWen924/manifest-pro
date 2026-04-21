package com.manifestreader.user.service.impl;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.user.model.dto.FileUploadInitRequest;
import com.manifestreader.user.model.vo.FileAssetVO;
import com.manifestreader.user.service.UserFileService;
import org.springframework.stereotype.Service;

@Service
public class UserFileServiceImpl implements UserFileService {

    @Override
    public FileAssetVO initUpload(FileUploadInitRequest request) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    @Override
    public FileAssetVO detail(Long id) {
        return new FileAssetVO(id, null, null, null, null, null);
    }

    @Override
    public void delete(Long id) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }
}
