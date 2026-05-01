package com.manifestreader.user.service;

import com.manifestreader.user.messaging.TemplateSaveTaskMessage;
import com.manifestreader.user.model.dto.TemplateExtractSaveRequest;
import com.manifestreader.user.model.vo.TemplateSaveTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateSaveTaskVO;

public interface TemplateSaveTaskService {

    TemplateSaveTaskSubmitVO submitTask(TemplateExtractSaveRequest request);

    TemplateSaveTaskVO getTask(String taskNo);

    void processTask(TemplateSaveTaskMessage message);
}
