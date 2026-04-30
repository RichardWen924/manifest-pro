package com.manifestreader.llmtask.controller;

import com.manifestreader.common.result.R;
import com.manifestreader.user.model.vo.BillExtractTaskSubmitVO;
import com.manifestreader.user.model.vo.BillExtractTaskVO;
import com.manifestreader.user.service.BillParseTaskService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/internal/llm/bills")
public class InternalBillTaskController {

    private final BillParseTaskService billParseTaskService;

    public InternalBillTaskController(BillParseTaskService billParseTaskService) {
        this.billParseTaskService = billParseTaskService;
    }

    @PostMapping(value = "/extract/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<BillExtractTaskSubmitVO> submitExtractTask(@RequestPart("file") MultipartFile file) {
        return R.ok(billParseTaskService.submitExtractTask(file));
    }

    @GetMapping("/extract/tasks/{taskNo}")
    public R<BillExtractTaskVO> getTask(@PathVariable String taskNo) {
        return R.ok(billParseTaskService.getTask(taskNo));
    }
}
