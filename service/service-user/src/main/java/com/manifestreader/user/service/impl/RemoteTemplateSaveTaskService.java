package com.manifestreader.user.service.impl;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.R;
import com.manifestreader.user.feign.LlmTaskFeignClient;
import com.manifestreader.user.messaging.TemplateSaveTaskMessage;
import com.manifestreader.user.model.dto.TemplateExtractSaveRequest;
import com.manifestreader.user.model.vo.TemplateSaveTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateSaveTaskVO;
import com.manifestreader.user.service.TemplateSaveTaskService;
import com.manifestreader.user.support.UserRequestContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class RemoteTemplateSaveTaskService implements TemplateSaveTaskService {

    private final LlmTaskFeignClient llmTaskFeignClient;
    private final UserRequestContext userRequestContext;

    public RemoteTemplateSaveTaskService(
            LlmTaskFeignClient llmTaskFeignClient,
            UserRequestContext userRequestContext
    ) {
        this.llmTaskFeignClient = llmTaskFeignClient;
        this.userRequestContext = userRequestContext;
    }

    @Override
    public TemplateSaveTaskSubmitVO submitTask(TemplateExtractSaveRequest request) {
        return unwrap(llmTaskFeignClient.submitTemplateSaveTask(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                request
        ));
    }

    @Override
    public TemplateSaveTaskVO getTask(String taskNo) {
        return unwrap(llmTaskFeignClient.getTemplateSaveTask(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                taskNo
        ));
    }

    @Override
    public void processTask(TemplateSaveTaskMessage message) {
        throw new UnsupportedOperationException("Template save processing is handled by llm-task service");
    }

    private <T> T unwrap(R<T> response) {
        if (response == null || !response.isSuccess()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), response == null ? "llm-task 服务不可用" : response.getMessage());
        }
        return response.getData();
    }
}
