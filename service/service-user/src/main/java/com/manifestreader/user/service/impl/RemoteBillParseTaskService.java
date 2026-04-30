package com.manifestreader.user.service.impl;

import com.manifestreader.common.exception.BusinessException;
import com.manifestreader.common.exception.ErrorCode;
import com.manifestreader.common.result.R;
import com.manifestreader.user.feign.LlmTaskFeignClient;
import com.manifestreader.user.messaging.BillParseTaskMessage;
import com.manifestreader.user.model.vo.BillExtractResultVO;
import com.manifestreader.user.model.vo.BillExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.BillExtractTaskVO;
import com.manifestreader.user.service.BillParseTaskService;
import com.manifestreader.user.support.UserRequestContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Primary
public class RemoteBillParseTaskService implements BillParseTaskService {

    private final LlmTaskFeignClient llmTaskFeignClient;
    private final UserRequestContext userRequestContext;

    public RemoteBillParseTaskService(LlmTaskFeignClient llmTaskFeignClient, UserRequestContext userRequestContext) {
        this.llmTaskFeignClient = llmTaskFeignClient;
        this.userRequestContext = userRequestContext;
    }

    @Override
    public BillExtractTaskSubmitVO submitExtractTask(MultipartFile file) {
        return unwrap(llmTaskFeignClient.submitBillExtractTask(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                file
        ));
    }

    @Override
    public BillExtractTaskVO getTask(String taskNo) {
        return unwrap(llmTaskFeignClient.getBillExtractTask(
                userRequestContext.currentCompanyId(),
                userRequestContext.currentUserId(),
                userRequestContext.currentTraceId(),
                taskNo
        ));
    }

    @Override
    public BillExtractResultVO resolveResult(String taskNo) {
        return getTask(taskNo).result();
    }

    @Override
    public void processTask(BillParseTaskMessage message) {
        throw new UnsupportedOperationException("Bill parse processing is handled by llm-task service");
    }

    private <T> T unwrap(R<T> response) {
        if (response == null || !response.isSuccess()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), response == null ? "llm-task 服务不可用" : response.getMessage());
        }
        return response.getData();
    }
}
