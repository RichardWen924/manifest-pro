package com.manifestreader.user.dify;

import org.springframework.web.multipart.MultipartFile;

public interface DifyWorkflowClient {

    String runTemplateExtraction(MultipartFile file);
}
