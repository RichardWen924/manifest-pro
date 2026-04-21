package com.manifestreader.user.service;

import com.manifestreader.user.model.dto.FileUploadInitRequest;
import com.manifestreader.user.model.vo.FileAssetVO;

public interface UserFileService {

    FileAssetVO initUpload(FileUploadInitRequest request);

    FileAssetVO detail(Long id);

    void delete(Long id);
}
