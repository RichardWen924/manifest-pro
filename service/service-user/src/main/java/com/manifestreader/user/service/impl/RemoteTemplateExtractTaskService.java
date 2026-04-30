package com.manifestreader.user.service.impl;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.R;
import com.manifestreader.user.feign.LlmTaskFeignClient;
import com.manifestreader.user.messaging.TemplateExtractTaskMessage;
import com.manifestreader.user.model.vo.BlankTemplateFile;
import com.manifestreader.user.model.vo.TemplateExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateExtractTaskVO;
import com.manifestreader.user.service.TemplateExtractTaskService;
import com.manifestreader.user.support.UserRequestContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Primary
public class RemoteTemplateExtractTaskService implements TemplateExtractTaskService {

    private final LlmTaskFeignClient llmTaskFeignClient;
    private final UserRequestContext userRequestContext;
    private final TemplateExtractTaskServiceImpl localTaskService;

    public RemoteTemplateExtractTaskService(
            LlmTaskFeignClient llmTaskFeignClient,
            UserRequestContext userRequestContext,
            TemplateExtractTaskServiceImpl localTaskService
    ) {
        this.llmTaskFeignClient = llmTaskFeignClient;
        this.userRequestContext = userRequestContext;
        this.localTaskService = localTaskService;
    }

    @Override
    public TemplateExtractTaskSubmitVO submitTask(MultipartFile file) {
        return unwrap(llmTaskFeignClient.submitTemplateExtractTask(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                file
        ));
    }

    @Override
    public TemplateExtractTaskVO getTask(String taskNo) {
        return unwrap(llmTaskFeignClient.getTemplateExtractTask(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                taskNo
        ));
    }

    @Override
    public BlankTemplateFile getBlankTemplate(String taskNo) {
        return localTaskService.getBlankTemplate(taskNo);
    }

    @Override
    public BlankTemplateFile getBlankTemplatePreview(String taskNo) {
        return localTaskService.getBlankTemplatePreview(taskNo);
    }

    @Override
    public void processTask(TemplateExtractTaskMessage message) {
        throw new UnsupportedOperationException("Template extract processing is handled by llm-task service");
    }

    private <T> T unwrap(R<T> response) {
        if (response == null || !response.isSuccess()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), response == null ? "llm-task 服务不可用" : response.getMessage());
        }
        return response.getData();
    }
}
