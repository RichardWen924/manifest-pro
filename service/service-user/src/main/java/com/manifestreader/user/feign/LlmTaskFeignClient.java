package com.manifestreader.user.feign;

import com.manifestreader.common.constant.HeaderConstants;
import com.manifestreader.common.result.R;
import com.manifestreader.user.model.vo.BillExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.BillExtractTaskVO;
import com.manifestreader.user.model.vo.TemplateExportTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateExportTaskVO;
import com.manifestreader.user.model.vo.TemplateExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateExtractTaskVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(
        name = "manifest-reader-llm-task",
        contextId = "llmTaskFeignClient",
        url = "${manifest.llm-task.base-url:http://127.0.0.1:8084}"
)
public interface LlmTaskFeignClient {

    @PostMapping(value = "/internal/llm/bills/extract/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<BillExtractTaskSubmitVO> submitBillExtractTask(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @RequestPart("file") MultipartFile file
    );

    @GetMapping("/internal/llm/bills/extract/tasks/{taskNo}")
    R<BillExtractTaskVO> getBillExtractTask(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("taskNo") String taskNo
    );

    @PostMapping(value = "/internal/llm/templates/extract/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<TemplateExtractTaskSubmitVO> submitTemplateExtractTask(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @RequestPart("file") MultipartFile file
    );

    @GetMapping("/internal/llm/templates/extract/tasks/{taskNo}")
    R<TemplateExtractTaskVO> getTemplateExtractTask(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("taskNo") String taskNo
    );

    @PostMapping(value = "/internal/llm/templates/export/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    R<TemplateExportTaskSubmitVO> submitTemplateExportTask(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @RequestParam("templateId") Long templateId,
            @RequestParam("outputFormat") String outputFormat,
            @RequestPart("file") MultipartFile file
    );

    @GetMapping("/internal/llm/templates/export/tasks/{taskNo}")
    R<TemplateExportTaskVO> getTemplateExportTask(
            @RequestHeader(HeaderConstants.COMPANY_ID) Long companyId,
            @RequestHeader(HeaderConstants.USER_ID) Long userId,
            @RequestHeader(HeaderConstants.TRACE_ID) String traceId,
            @PathVariable("taskNo") String taskNo
    );
}
