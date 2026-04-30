package com.manifestreader.llmtask.controller;

import com.manifestreader.common.result.R;
import com.manifestreader.user.model.vo.TemplateExportTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateExportTaskVO;
import com.manifestreader.user.model.vo.TemplateExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.TemplateExtractTaskVO;
import com.manifestreader.user.service.TemplateExportTaskService;
import com.manifestreader.user.service.TemplateExtractTaskService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/internal/llm/templates")
public class InternalTemplateTaskController {

    private final TemplateExtractTaskService templateExtractTaskService;
    private final TemplateExportTaskService templateExportTaskService;

    public InternalTemplateTaskController(
            TemplateExtractTaskService templateExtractTaskService,
            TemplateExportTaskService templateExportTaskService
    ) {
        this.templateExtractTaskService = templateExtractTaskService;
        this.templateExportTaskService = templateExportTaskService;
    }

    @PostMapping(value = "/extract/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<TemplateExtractTaskSubmitVO> submitExtractTask(@RequestPart("file") MultipartFile file) {
        return R.ok(templateExtractTaskService.submitTask(file));
    }

    @GetMapping("/extract/tasks/{taskNo}")
    public R<TemplateExtractTaskVO> getExtractTask(@PathVariable String taskNo) {
        return R.ok(templateExtractTaskService.getTask(taskNo));
    }

    @PostMapping(value = "/export/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<TemplateExportTaskSubmitVO> submitExportTask(
            @RequestParam("templateId") Long templateId,
            @RequestParam(value = "outputFormat", defaultValue = "DOCX") String outputFormat,
            @RequestPart("file") MultipartFile file
    ) {
        return R.ok(templateExportTaskService.submitTask(templateId, outputFormat, file));
    }

    @GetMapping("/export/tasks/{taskNo}")
    public R<TemplateExportTaskVO> getExportTask(@PathVariable String taskNo) {
        return R.ok(templateExportTaskService.getTask(taskNo));
    }
}
