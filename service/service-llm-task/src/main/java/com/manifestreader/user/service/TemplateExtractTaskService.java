package com.manifestreader.user.service;

import com.manifestreader.user.messaging.TemplateExtractTaskMessage;
import com.manifestreader.user.model.vo.BlankTemplateFile;
import com.manifestreader.user.model.vo.TemplateExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateExtractTaskVO;
import org.springframework.web.multipart.MultipartFile;

public interface TemplateExtractTaskService {

    TemplateExtractTaskSubmitVO submitTask(MultipartFile file);

    TemplateExtractTaskVO getTask(String taskNo);

    BlankTemplateFile getBlankTemplate(String taskNo);

    BlankTemplateFile getBlankTemplatePreview(String taskNo);

    void processTask(TemplateExtractTaskMessage message);
}
