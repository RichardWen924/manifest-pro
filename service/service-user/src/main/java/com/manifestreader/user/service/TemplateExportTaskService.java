package com.manifestreader.user.service;

import com.manifestreader.user.messaging.TemplateExportTaskMessage;
import com.manifestreader.user.model.vo.ExportedTemplateFile;
import com.manifestreader.user.model.vo.TemplateExportTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateExportTaskVO;
import org.springframework.web.multipart.MultipartFile;

public interface TemplateExportTaskService {

    TemplateExportTaskSubmitVO submitTask(Long templateId, String outputFormat, MultipartFile file);

    TemplateExportTaskVO getTask(String taskNo);

    ExportedTemplateFile getExportedFile(String taskNo);

    void processTask(TemplateExportTaskMessage message);
}
