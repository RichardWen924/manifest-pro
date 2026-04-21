package com.manifestreader.admin.controller.dict;

import com.manifestreader.admin.service.dict.DictAdminService;
import com.manifestreader.common.result.Result;
import com.manifestreader.model.entity.DictItemEntity;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dict")
public class DictController {

    private final DictAdminService dictAdminService;

    public DictController(DictAdminService dictAdminService) {
        this.dictAdminService = dictAdminService;
    }

    @GetMapping("/items")
    public Result<List<DictItemEntity>> listItems(@RequestParam String dictType) {
        return Result.success(dictAdminService.listItems(dictType));
    }
}
