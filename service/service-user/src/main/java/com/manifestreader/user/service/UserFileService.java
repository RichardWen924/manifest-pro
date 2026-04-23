package com.manifestreader.user.service;

import com.manifestreader.user.model.dto.FileUploadInitRequest;
import com.manifestreader.user.model.vo.FileAssetVO;
import com.manifestreader.user.model.vo.FileDownloadVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserFileService {

    FileAssetVO initUpload(FileUploadInitRequest request);

    FileAssetVO upload(MultipartFile file, String bizType);

    FileAssetVO detail(Long id);

    FileDownloadVO download(Long id);

    void delete(Long id);
}
