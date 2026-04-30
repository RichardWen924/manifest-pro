package com.manifestreader.user.service.impl;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.R;
import com.manifestreader.user.feign.LlmTaskFeignClient;
import com.manifestreader.user.messaging.TemplateExportTaskMessage;
import com.manifestreader.user.model.vo.ExportedTemplateFile;
import com.manifestreader.user.model.vo.TemplateExportTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateExportTaskVO;
import com.manifestreader.user.service.TemplateExportTaskService;
import com.manifestreader.user.support.UserRequestContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Primary
public class RemoteTemplateExportTaskService implements TemplateExportTaskService {

    private final LlmTaskFeignClient llmTaskFeignClient;
    private final UserRequestContext userRequestContext;
    private final TemplateExportTaskServiceImpl localTaskService;

    public RemoteTemplateExportTaskService(
            LlmTaskFeignClient llmTaskFeignClient,
            UserRequestContext userRequestContext,
            TemplateExportTaskServiceImpl localTaskService
    ) {
        this.llmTaskFeignClient = llmTaskFeignClient;
        this.userRequestContext = userRequestContext;
        this.localTaskService = localTaskService;
    }

    @Override
    public TemplateExportTaskSubmitVO submitTask(Long templateId, String outputFormat, MultipartFile file) {
        return unwrap(llmTaskFeignClient.submitTemplateExportTask(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                templateId,
                outputFormat,
                file
        ));
    }

    @Override
    public TemplateExportTaskVO getTask(String taskNo) {
        return unwrap(llmTaskFeignClient.getTemplateExportTask(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                taskNo
        ));
    }

    @Override
    public ExportedTemplateFile getExportedFile(String taskNo) {
        return localTaskService.getExportedFile(taskNo);
    }

    @Override
    public void processTask(TemplateExportTaskMessage message) {
        throw new UnsupportedOperationException("Template export processing is handled by llm-task service");
    }

    private <T> T unwrap(R<T> response) {
        if (response == null || !response.isSuccess()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), response == null ? "llm-task 服务不可用" : response.getMessage());
        }
        return response.getData();
    }
}
