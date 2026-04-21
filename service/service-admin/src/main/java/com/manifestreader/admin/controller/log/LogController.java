package com.manifestreader.admin.controller.log;

import com.manifestreader.admin.service.log.LogAdminService;
import com.manifestreader.common.result.PageResult;
import com.manifestreader.common.result.Result;
import com.manifestreader.model.entity.OperLogEntity;
import com.manifestreader.model.query.BasePageQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/log")
public class LogController {

    private final LogAdminService logAdminService;

    public LogController(LogAdminService logAdminService) {
        this.logAdminService = logAdminService;
    }

    @GetMapping("/page")
    public Result<PageResult<OperLogEntity>> page(BasePageQuery query) {
        return Result.success(logAdminService.pageLogs(query));
    }
}
